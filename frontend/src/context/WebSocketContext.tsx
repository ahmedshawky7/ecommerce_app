import React, { createContext, useContext, useEffect, useState } from 'react';
import { Client } from '@stomp/stompjs';
import { useQueryClient } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import { useAuth } from './AuthContext';
import { AdminAlertEvent, OrderStatusEvent } from '../types/api';

const WS_URL =
    import.meta.env.VITE_WS_URL ??
    (window.location.protocol === 'https:'
        ? `wss://${window.location.host}/ws`
        : 'ws://localhost:8080/ws');

interface WebSocketContextValue {
    connected: boolean;
    adminAlerts: AdminAlertEvent[];
    clearAdminAlerts: () => void;
}

const WebSocketContext = createContext<WebSocketContextValue>({
    connected: false,
    adminAlerts: [],
    clearAdminAlerts: () => { },
});

export function WebSocketProvider({ children }: { children: React.ReactNode }) {
    const { user, isAuthenticated, isAdmin } = useAuth();
    const queryClient = useQueryClient();
    const [connected, setConnected] = useState(false);
    const [adminAlerts, setAdminAlerts] = useState<AdminAlertEvent[]>([]);

    useEffect(() => {
        if (!isAuthenticated || !user?.token) {
            setConnected(false);
            return;
        }

        const client = new Client({
            brokerURL: WS_URL,
            connectHeaders: {
                Authorization: `Bearer ${user.token}`,
            },
            reconnectDelay: 5000,
            onConnect: () => {
                setConnected(true);

                client.subscribe('/user/queue/orders', (frame) => {
                    try {
                        const event: OrderStatusEvent = JSON.parse(frame.body);
                        toast.success(`${event.orderNumber}: ${event.message}`, { duration: 5000 });
                        queryClient.invalidateQueries({ queryKey: ['my-orders'] });
                        queryClient.invalidateQueries({ queryKey: ['order-detail', String(event.orderId)] });
                    } catch {
                        // ignore malformed frame
                    }
                });

                if (isAdmin) {
                    client.subscribe('/topic/admin/alerts', (frame) => {
                        try {
                            const alert: AdminAlertEvent = JSON.parse(frame.body);
                            setAdminAlerts((prev) => [alert, ...prev].slice(0, 20));
                            toast(`${alert.title}: ${alert.message}`, {
                                icon: alert.type === 'LOW_STOCK' ? '⚠️' : '🔔',
                                duration: 5000,
                            });
                            queryClient.invalidateQueries({ queryKey: ['admin-stats'] });
                            queryClient.invalidateQueries({ queryKey: ['admin-orders'] });
                            queryClient.invalidateQueries({ queryKey: ['admin-low-stock'] });
                        } catch {
                            // ignore malformed frame
                        }
                    });
                }
            },
            onDisconnect: () => setConnected(false),
            onStompError: () => setConnected(false),
        });

        client.activate();

        return () => {
            client.deactivate();
            setConnected(false);
        };
    }, [isAuthenticated, isAdmin, user?.token, queryClient]);

    return (
        <WebSocketContext.Provider
            value={{
                connected,
                adminAlerts,
                clearAdminAlerts: () => setAdminAlerts([]),
            }}
        >
            {children}
        </WebSocketContext.Provider>
    );
}

export function useWebSocket() {
    return useContext(WebSocketContext);
}