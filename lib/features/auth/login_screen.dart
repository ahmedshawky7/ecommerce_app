import 'package:animated_snack_bar/animated_snack_bar.dart';
import 'package:ecommerce_app/core/routing/app_routes.dart';
import 'package:ecommerce_app/core/styling/app_styles.dart';
import 'package:ecommerce_app/core/utils/animated_snack_dialog.dart';
import 'package:ecommerce_app/core/utils/service_locator.dart';
import 'package:ecommerce_app/core/utils/storage_helper.dart';
import 'package:ecommerce_app/core/widgets/custom_text_field.dart';
import 'package:ecommerce_app/core/widgets/loading_widget.dart';
import 'package:ecommerce_app/core/widgets/primary_button_widget.dart';
import 'package:ecommerce_app/core/widgets/spacing_widgets.dart';
import 'package:ecommerce_app/features/auth/cubit/auth_cubit.dart';
import 'package:ecommerce_app/features/auth/cubit/auth_state.dart';
import 'package:flutter/gestures.dart';
import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:go_router/go_router.dart';

class LoginScreen extends StatefulWidget {
  const LoginScreen({super.key});

  @override
  State<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends State<LoginScreen> {
  bool isPasswordVisible = true;
  final formKey = GlobalKey<FormState>();
  final TextEditingController usernameController = TextEditingController();
  final TextEditingController passwordController = TextEditingController();

  @override
  void initState() {
    super.initState();
    sl<StorageHelper>().getToken().then((value) {
      if (value != null && value.isNotEmpty) {
        context.goNamed(AppRoutes.mainScreen);
      }
    });
  }

  void _triggerLogin() {
    if (formKey.currentState!.validate()) {
      context.read<AuthCubit>().login(
            username: usernameController.text,
            password: passwordController.text,
          );
    }
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Scaffold(
      backgroundColor: theme.scaffoldBackgroundColor,
      body: BlocConsumer<AuthCubit, AuthState>(
        listener: (context, state) {
          if (state is AuthErrorState) {
            showAnimatedSnackDialog(
              context,
              message: state.error,
              type: AnimatedSnackBarType.error,
            );
          } else if (state is AuthSuccessState) {
            showAnimatedSnackDialog(
              context,
              message: state.message,
              type: AnimatedSnackBarType.success,
            );
            context.goNamed(AppRoutes.mainScreen);
          }
        },
        builder: (context, state) {
          return Stack(
            children: [
              SafeArea(
                child: SingleChildScrollView(
                  padding: EdgeInsets.symmetric(horizontal: 22.w),
                  child: Form(
                    key: formKey,
                    child: AutofillGroup(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          HeightSpace(28),
                          Text(
                            'Login to your account',
                            style: AppStyles.primaryHeadLineStyle(context)
                                .copyWith(color: theme.textTheme.titleLarge?.color, fontSize: 32.sp),
                          ),
                          HeightSpace(8),
                          Text(
                            'It’s great to see you again.',
                            style: AppStyles.subTitleStyle(context)
                                .copyWith(color: theme.textTheme.titleMedium?.color),
                          ),
                          HeightSpace(32),

                          // Username
                          Text(
                            'Username',
                            style: AppStyles.black16w500Style(context)
                                .copyWith(color: theme.colorScheme.onBackground),
                          ),
                          HeightSpace(4),
                          CustomTextField(
                            width: double.infinity,
                            hintText: 'Enter your email address',
                            controller: usernameController,
                            keyboardType: TextInputType.emailAddress,
                            autofillHints: const [AutofillHints.email],
                            textInputAction: TextInputAction.next,
                            validator: (value) {
                              if (value == null || value.isEmpty) {
                                return 'Please enter your email address';
                              }
                              return null;
                            },
                          ),
                          HeightSpace(16),

                          // Password
                          Text(
                            'Password',
                            style: AppStyles.black16w500Style(context)
                                .copyWith(color: theme.colorScheme.onBackground),
                          ),
                          HeightSpace(4),
                          CustomTextField(
                            width: double.infinity,
                            hintText: 'Enter your password',
                            controller: passwordController,
                            isPassword: isPasswordVisible,
                            keyboardType: TextInputType.visiblePassword,
                            autofillHints: const [AutofillHints.password],
                            textInputAction: TextInputAction.done,
                            onFieldSubmitted: (_) => _triggerLogin(),
                            suffixIcon: IconButton(
                              icon: Icon(
                                isPasswordVisible ? Icons.visibility_off : Icons.visibility,
                                color: theme.colorScheme.secondary, 
                                size: 20.sp,
                              ),
                              onPressed: () {
                                setState(() {
                                  isPasswordVisible = !isPasswordVisible;
                                });
                              },
                            ),
                            validator: (value) {
                              if (value == null || value.isEmpty) {
                                return 'Please enter your password';
                              } else if (value.length < 6) {
                                return 'Password must be at least 6 characters';
                              }
                              return null;
                            },
                          ),
                          HeightSpace(55),

                          // Login Button
                          PrimaryButtonWidget(
                            width: double.infinity,
                            buttonText: 'Sign In',
                            onPressed: _triggerLogin,
                          ),
                          HeightSpace(24),

                          // Register Link
                          Center(
                            child: RichText(
                              text: TextSpan(
                                text: 'Don\'t have an account? ',
                                style: AppStyles.grey12MediumStyle(context)
                                    .copyWith(color: theme.colorScheme.secondary),
                                children: [
                                  TextSpan(
                                    text: 'Join',
                                    style: AppStyles.black15BoldStyle(context)
                                        .copyWith(color: theme.colorScheme.primary),
                                    recognizer: TapGestureRecognizer()
                                      ..onTap = () {
                                        GoRouter.of(context)
                                            .pushNamed(AppRoutes.registerScreen);
                                      },
                                  ),
                                ],
                              ),
                            ),
                          ),
                          HeightSpace(16),
                        ],
                      ),
                    ),
                  ),
                ),
              ),

              // Loading overlay
              if (state is AuthLoadingState) const LoadingWidget(),
            ],
          );
        },
      ),
    );
  }
}