abstract class AuthState {}

class AuthInitialState extends AuthState {}

class AuthLoadingState extends AuthState {}
class AuthLoggedOutState extends AuthState {}

class AuthSuccessState extends AuthState {
  String message;
  AuthSuccessState(this.message);
}

class AuthErrorState extends AuthState {
  String error;
  AuthErrorState(this.error);
}