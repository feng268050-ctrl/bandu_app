import 'package:flutter/material.dart';

class TutorPage extends StatelessWidget {
  const TutorPage({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('AI 辅导')),
      body: const Center(child: Text('暂无会话')),
    );
  }
}
