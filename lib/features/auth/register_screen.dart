import 'package:ecommerce_app/core/styling/app_styles.dart';
import 'package:ecommerce_app/core/widgets/custom_text_field.dart';
import 'package:ecommerce_app/core/widgets/primary_button_widget.dart';
import 'package:ecommerce_app/core/widgets/spacing_widgets.dart';
import 'package:flutter/gestures.dart';
import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:go_router/go_router.dart';

class RegisterScreen extends StatefulWidget {
  const RegisterScreen({super.key});

  @override
  State<RegisterScreen> createState() => _RegisterScreenState();
}

class _RegisterScreenState extends State<RegisterScreen> {
  bool isPasswordVisible = true;
  bool isConfirmPasswordVisible = true;
  final formKey = GlobalKey<FormState>();

  final TextEditingController fullNameController = TextEditingController();
  final TextEditingController emailController = TextEditingController();
  final TextEditingController passwordController = TextEditingController();
  final TextEditingController confirmPasswordController = TextEditingController();

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Scaffold(
      backgroundColor: theme.scaffoldBackgroundColor,
      body: SafeArea(
        child: SingleChildScrollView(
          padding: EdgeInsets.symmetric(horizontal: 22.w),
          child: Form(
            key: formKey,
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                HeightSpace(28),
                Text(
                  'Create an account',
                  style: AppStyles.primaryHeadLineStyle(context)
                      .copyWith(color: theme.textTheme.titleLarge?.color),
                ),
                HeightSpace(4),
                Text(
                  'Let’s create your account.',
                  style: AppStyles.subTitleStyle(context)
                      .copyWith(color: theme.textTheme.titleMedium?.color),
                ),
                HeightSpace(24),

                // Full Name
                Text(
                  'Full Name',
                  style: AppStyles.black16w500Style(context)
                      .copyWith(color: theme.colorScheme.onBackground),
                ),
                HeightSpace(4),
                CustomTextField(
                  width: double.infinity,
                  hintText: 'Enter your full name',
                  controller: fullNameController,
                  keyboardType: TextInputType.name,
                  autofillHints: const [AutofillHints.name],
                  textInputAction: TextInputAction.next,
                  validator: (value) {
                    if (value == null || value.isEmpty) {
                      return 'Please enter your full name';
                    } else if (value.length < 3) {
                      return 'Full name must be at least 3 characters';
                    }
                    return null;
                  },
                ),
                HeightSpace(16),

                // Email
                Text(
                  'Email',
                  style: AppStyles.black16w500Style(context)
                      .copyWith(color: theme.colorScheme.onBackground),
                ),
                HeightSpace(4),
                CustomTextField(
                  width: double.infinity,
                  hintText: 'Enter your email address',
                  controller: emailController,
                  keyboardType: TextInputType.emailAddress,
                  autofillHints: const [AutofillHints.email],
                  textInputAction: TextInputAction.next,
                  validator: (value) {
                    if (value == null || value.isEmpty) {
                      return 'Please enter your email';
                    } else if (!value.contains('@') || value.length < 5) {
                      return 'Please enter a valid email';
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
                  textInputAction: TextInputAction.next,
                  suffixIcon: IconButton(
                    icon: Icon(
                      isPasswordVisible ? Icons.visibility_off : Icons.visibility,
                      color: theme.colorScheme.secondary, // استخدم لون ديناميكي
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
                HeightSpace(16),

                // Confirm Password
                Text(
                  'Confirm Password',
                  style: AppStyles.black16w500Style(context)
                      .copyWith(color: theme.colorScheme.onBackground),
                ),
                HeightSpace(4),
                CustomTextField(
                  width: double.infinity,
                  hintText: 'Confirm your password',
                  controller: confirmPasswordController,
                  isPassword: isConfirmPasswordVisible,
                  keyboardType: TextInputType.visiblePassword,
                  autofillHints: const [AutofillHints.password],
                  textInputAction: TextInputAction.done,
                  suffixIcon: IconButton(
                    icon: Icon(
                      isConfirmPasswordVisible ? Icons.visibility_off : Icons.visibility,
                      color: theme.colorScheme.secondary,
                      size: 20.sp,
                    ),
                    onPressed: () {
                      setState(() {
                        isConfirmPasswordVisible = !isConfirmPasswordVisible;
                      });
                    },
                  ),
                  validator: (value) {
                    if (value == null || value.isEmpty) {
                      return 'Please confirm your password';
                    } else if (value != passwordController.text) {
                      return 'Passwords do not match';
                    }
                    return null;
                  },
                ),
                HeightSpace(30),

                PrimaryButtonWidget(
                  width: double.infinity,
                  buttonText: 'Create Account',
                  onPressed: () {
                    if (formKey.currentState!.validate()) {
                      // هنا ضيف منطق التسجيل
                    }
                  },
                ),
                HeightSpace(24),

                Center(
                  child: RichText(
                    text: TextSpan(
                      text: 'Already have an account? ',
                      style: AppStyles.grey12MediumStyle(context)
                          .copyWith(color: theme.colorScheme.secondary),
                      children: [
                        TextSpan(
                          text: 'Login Now',
                          style: AppStyles.black15BoldStyle(context)
                              .copyWith(color: theme.colorScheme.primary),
                          recognizer: TapGestureRecognizer()
                            ..onTap = () {
                              context.pop();
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
    );
  }
}