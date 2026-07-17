import 'package:bandu_wrong_notebook/components/design_system/tokens/app_duration.dart';
import 'package:bandu_wrong_notebook/components/design_system/tokens/app_sizes.dart';
import 'package:bandu_wrong_notebook/components/design_system/tokens/app_spacing.dart';
import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

class AppShell extends StatefulWidget {
  const AppShell({
    required this.navigationShell,
    required this.showPrimaryNavigation,
    super.key,
  });

  final StatefulNavigationShell navigationShell;
  final bool showPrimaryNavigation;

  @override
  State<AppShell> createState() => _AppShellState();
}

class _AppShellState extends State<AppShell>
    with SingleTickerProviderStateMixin {
  late final AnimationController _fabVisibility;
  late final CurvedAnimation _fabCurved;
  late final Animation<double> _fabScale;
  late final Animation<double> _fabRotation;

  @override
  void initState() {
    super.initState();
    _fabVisibility = AnimationController(
      vsync: this,
      duration: AppDuration.navigationFab,
      value: widget.showPrimaryNavigation ? 1 : 0,
    )..addListener(() {
        if (mounted) {
          setState(() {});
        }
      });
    _fabCurved = CurvedAnimation(
      parent: _fabVisibility,
      curve: Curves.easeOutCubic,
      reverseCurve: Curves.easeInCubic,
    );
    _fabScale = Tween<double>(begin: 0.72, end: 1).animate(_fabCurved);
    // Match Material FAB turn interval (45°); reverse plays on exit.
    _fabRotation = Tween<double>(
      begin: -kFloatingActionButtonTurnInterval,
      end: 0,
    ).animate(_fabCurved);
  }

  @override
  void didUpdateWidget(AppShell oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (widget.showPrimaryNavigation == oldWidget.showPrimaryNavigation) {
      return;
    }
    if (widget.showPrimaryNavigation) {
      _fabVisibility.forward();
    } else {
      _fabVisibility.reverse();
    }
  }

  @override
  void dispose() {
    _fabCurved.dispose();
    _fabVisibility.dispose();
    super.dispose();
  }

  void _goToBranch(int index) {
    widget.navigationShell.goBranch(
      index,
      initialLocation: index == widget.navigationShell.currentIndex,
    );
  }

  Widget? _buildCaptureFab() {
    if (_fabVisibility.status == AnimationStatus.dismissed) {
      return null;
    }

    return FadeTransition(
      opacity: _fabCurved,
      child: ScaleTransition(
        scale: _fabScale,
        child: RotationTransition(
          turns: _fabRotation,
          child: AppCaptureNavigationButton(
            onPressed: () => _goToBranch(_captureDestinationIndex),
          ),
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: SafeArea(child: widget.navigationShell),
      floatingActionButtonLocation: appCaptureNavigationButtonLocation,
      floatingActionButtonAnimator: FloatingActionButtonAnimator.noAnimation,
      floatingActionButton: _buildCaptureFab(),
      bottomNavigationBar: widget.showPrimaryNavigation
          ? AppBottomNavigationBar(
              selectedIndex: widget.navigationShell.currentIndex,
              onDestinationSelected: _goToBranch,
            )
          : null,
    );
  }
}

const primaryNavigationLocations = {
  '/home',
  '/tutor',
  '/capture',
  '/practice',
  '/profile',
};

bool shouldShowPrimaryNavigation(String location) {
  return primaryNavigationLocations.contains(Uri.parse(location).path);
}

const _captureDestinationIndex = 2;

const FloatingActionButtonLocation appCaptureNavigationButtonLocation =
    _BottomNavigationCenteredFabLocation();

class _BottomNavigationCenteredFabLocation
    extends FloatingActionButtonLocation {
  const _BottomNavigationCenteredFabLocation();

  @override
  Offset getOffset(ScaffoldPrelayoutGeometry scaffoldGeometry) {
    final dockedOffset = FloatingActionButtonLocation.centerDocked.getOffset(
      scaffoldGeometry,
    );

    return dockedOffset.translate(
      0,
      AppSizes.primaryNavigationLabelAlignmentOffset,
    );
  }
}

const _destinations = [
  _AppDestination(
    index: 0,
    icon: Icons.home_outlined,
    selectedIcon: Icons.home,
    label: '首页',
  ),
  _AppDestination(
    index: 1,
    icon: Icons.forum_outlined,
    selectedIcon: Icons.forum,
    label: 'AI 辅导',
  ),
  _AppDestination(
    index: 3,
    icon: Icons.quiz_outlined,
    selectedIcon: Icons.quiz,
    label: '练习',
  ),
  _AppDestination(
    index: 4,
    icon: Icons.person_outline,
    selectedIcon: Icons.person,
    label: '我的',
  ),
];

class AppBottomNavigationBar extends StatelessWidget {
  const AppBottomNavigationBar({
    required this.selectedIndex,
    required this.onDestinationSelected,
    super.key,
  });

  final int selectedIndex;
  final ValueChanged<int> onDestinationSelected;

  @override
  Widget build(BuildContext context) {
    final colorScheme = Theme.of(context).colorScheme;

    return BottomAppBar(
      height: AppSizes.bottomNavigationHeight,
      padding: EdgeInsets.zero,
      color: colorScheme.surfaceContainer,
      elevation: 8,
      child: Stack(
        children: [
          Positioned(
            top: 0,
            left: 0,
            right: 0,
            child: Divider(
              height: 1,
              thickness: 1,
              color: colorScheme.outlineVariant,
            ),
          ),
          Row(
            children: [
              _NavigationItem(
                destination: _destinations[0],
                selected: selectedIndex == _destinations[0].index,
                onTap: onDestinationSelected,
              ),
              _NavigationItem(
                destination: _destinations[1],
                selected: selectedIndex == _destinations[1].index,
                onTap: onDestinationSelected,
              ),
              const Expanded(child: SizedBox.shrink()),
              _NavigationItem(
                destination: _destinations[2],
                selected: selectedIndex == _destinations[2].index,
                onTap: onDestinationSelected,
              ),
              _NavigationItem(
                destination: _destinations[3],
                selected: selectedIndex == _destinations[3].index,
                onTap: onDestinationSelected,
              ),
            ],
          ),
        ],
      ),
    );
  }
}

class AppCaptureNavigationButton extends StatelessWidget {
  const AppCaptureNavigationButton({required this.onPressed, super.key});

  final VoidCallback onPressed;

  @override
  Widget build(BuildContext context) {
    final colorScheme = Theme.of(context).colorScheme;

    return SizedBox.square(
      dimension: AppSizes.primaryNavigationCradle,
      child: DecoratedBox(
        decoration: BoxDecoration(
          color: colorScheme.surfaceContainer,
          shape: BoxShape.circle,
        ),
        child: Center(
          child: SizedBox.square(
            dimension: AppSizes.primaryNavigationAction,
            child: FloatingActionButton(
              heroTag: 'capture-navigation-action',
              tooltip: '拍题',
              onPressed: onPressed,
              elevation: 0,
              focusElevation: 2,
              hoverElevation: 2,
              highlightElevation: 2,
              backgroundColor: colorScheme.primary,
              foregroundColor: colorScheme.onPrimary,
              shape: const CircleBorder(),
              child: const Icon(Icons.add_rounded, size: 40),
            ),
          ),
        ),
      ),
    );
  }
}

class _NavigationItem extends StatelessWidget {
  const _NavigationItem({
    required this.destination,
    required this.selected,
    required this.onTap,
  });

  final _AppDestination destination;
  final bool selected;
  final ValueChanged<int> onTap;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final color = selected
        ? theme.colorScheme.primary
        : theme.colorScheme.onSurfaceVariant;
    final labelStyle = theme.textTheme.labelSmall?.copyWith(
      color: color,
      fontWeight: selected ? FontWeight.w700 : FontWeight.w500,
    );

    return Expanded(
      child: Semantics(
        button: true,
        selected: selected,
        label: destination.label,
        excludeSemantics: true,
        child: Tooltip(
          message: destination.label,
          child: InkWell(
            onTap: () => onTap(destination.index),
            child: SizedBox.expand(
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Icon(
                    selected ? destination.selectedIcon : destination.icon,
                    size: AppSizes.bottomNavigationIcon,
                    color: color,
                  ),
                  const SizedBox(height: AppSpacing.xSmall),
                  AnimatedDefaultTextStyle(
                    duration: AppDuration.short,
                    style: labelStyle ?? TextStyle(color: color),
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    child: Text(destination.label),
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}

class _AppDestination {
  const _AppDestination({
    required this.index,
    required this.icon,
    required this.selectedIcon,
    required this.label,
  });

  final int index;
  final IconData icon;
  final IconData selectedIcon;
  final String label;
}
