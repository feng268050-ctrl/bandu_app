import 'package:bandu_wrong_notebook/features/auth/presentation/auth_controller.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

class ProfilePage extends ConsumerWidget {
  const ProfilePage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final authState = ref.watch(authControllerProvider);
    final user = authState.user;

    return Scaffold(
      appBar: AppBar(title: const Text('我的')),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          ListTile(
            contentPadding: EdgeInsets.zero,
            leading: const CircleAvatar(child: Icon(Icons.person)),
            title: Text(user?.name ?? user?.email ?? '未登录'),
            subtitle: user?.email == null ? null : Text(user!.email),
          ),
          const Divider(height: 32),
          ListTile(
            leading: const Icon(Icons.bar_chart_outlined),
            title: const Text('统计'),
            onTap: () {},
          ),
          ListTile(
            leading: const Icon(Icons.sell_outlined),
            title: const Text('标签'),
            onTap: () {},
          ),
          const SizedBox(height: 12),
          OutlinedButton.icon(
            onPressed: authState.isBusy
                ? null
                : () => ref.read(authControllerProvider.notifier).logout(),
            icon: const Icon(Icons.logout),
            label: const Text('退出登录'),
          ),
        ],
      ),
    );
  }
}
