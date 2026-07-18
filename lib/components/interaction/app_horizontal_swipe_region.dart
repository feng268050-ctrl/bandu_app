import 'package:flutter/material.dart';

class AppHorizontalSwipeRegion extends StatelessWidget {
  const AppHorizontalSwipeRegion({
    required this.child,
    required this.onSwipeRight,
    this.minimumVelocity = 280,
    super.key,
  });

  final Widget child;
  final VoidCallback onSwipeRight;
  final double minimumVelocity;

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      behavior: HitTestBehavior.translucent,
      onHorizontalDragEnd: (details) {
        if ((details.primaryVelocity ?? 0) > minimumVelocity) {
          onSwipeRight();
        }
      },
      child: child,
    );
  }
}
