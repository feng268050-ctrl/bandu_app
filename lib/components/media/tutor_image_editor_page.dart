import 'dart:io';
import 'dart:math' as math;
import 'dart:ui' as ui;

import 'package:bandu_wrong_notebook/components/design_system/tokens/app_spacing.dart';
import 'package:flutter/material.dart';
import 'package:path/path.dart' as p;
import 'package:path_provider/path_provider.dart';

/// Full-screen editor with crop + freehand brush. Pops the saved image path.
class TutorImageEditorPage extends StatefulWidget {
  const TutorImageEditorPage({required this.imagePath, super.key});

  final String imagePath;

  @override
  State<TutorImageEditorPage> createState() => _TutorImageEditorPageState();
}

enum _EditorTool { crop, brush }

enum _CropHandle { nw, n, ne, e, se, s, sw, w, body }

class _Stroke {
  _Stroke({required this.color, required this.width});

  final Color color;
  final double width;
  final List<Offset> points = <Offset>[];
}

class _TutorImageEditorPageState extends State<TutorImageEditorPage> {
  static const _minCropSize = 24.0;
  static const _handleHitSize = 28.0;

  ui.Image? _image;
  String? _loadError;
  bool _saving = false;
  _EditorTool _tool = _EditorTool.brush;
  final List<_Stroke> _strokes = <_Stroke>[];
  _Stroke? _activeStroke;
  Color _brushColor = const Color(0xFFE53935);
  double _brushWidth = 4;
  Rect? _cropRect;
  Size? _viewportSize;
  _CropHandle? _activeCropHandle;
  Rect? _cropRectAtDragStart;
  Offset? _cropDragStartImagePoint;

  static const _brushColors = <Color>[
    Color(0xFFE53935),
    Color(0xFFFFB300),
    Color(0xFF1E88E5),
    Color(0xFFFFFFFF),
    Color(0xFF212121),
  ];

  @override
  void initState() {
    super.initState();
    _loadImage();
  }

  @override
  void dispose() {
    _image?.dispose();
    super.dispose();
  }

  Future<void> _loadImage() async {
    try {
      final bytes = await File(widget.imagePath).readAsBytes();
      final codec = await ui.instantiateImageCodec(bytes);
      final frame = await codec.getNextFrame();
      if (!mounted) {
        frame.image.dispose();
        return;
      }
      setState(() {
        _image?.dispose();
        _image = frame.image;
        _cropRect = null;
        _strokes.clear();
        _loadError = null;
      });
    } catch (_) {
      if (!mounted) return;
      setState(() => _loadError = '无法打开图片');
    }
  }

  Rect _imageDisplayRect(Size viewport, ui.Image image) {
    final imageSize = Size(image.width.toDouble(), image.height.toDouble());
    final fitted = applyBoxFit(BoxFit.contain, imageSize, viewport);
    final output = fitted.destination;
    final dx = (viewport.width - output.width) / 2;
    final dy = (viewport.height - output.height) / 2;
    return Rect.fromLTWH(dx, dy, output.width, output.height);
  }

  Offset? _toImagePoint(Offset local, Size viewport, ui.Image image) {
    final display = _imageDisplayRect(viewport, image);
    final nx = ((local.dx - display.left) / display.width).clamp(0.0, 1.0);
    final ny = ((local.dy - display.top) / display.height).clamp(0.0, 1.0);
    return Offset(nx * image.width, ny * image.height);
  }

  Rect _cropDisplayRect(Rect crop, Rect display, ui.Image image) {
    return Rect.fromLTWH(
      display.left + crop.left / image.width * display.width,
      display.top + crop.top / image.height * display.height,
      crop.width / image.width * display.width,
      crop.height / image.height * display.height,
    );
  }

  Rect _defaultCropRect(ui.Image image) {
    final insetX = image.width * 0.08;
    final insetY = image.height * 0.08;
    return Rect.fromLTRB(
      insetX,
      insetY,
      image.width - insetX,
      image.height - insetY,
    );
  }

  Rect _ensureCropRect(ui.Image image) {
    return _cropRect ??= _defaultCropRect(image);
  }

  Map<_CropHandle, Offset> _handleCenters(Rect cropDisplay) {
    return {
      _CropHandle.nw: cropDisplay.topLeft,
      _CropHandle.n: Offset(cropDisplay.center.dx, cropDisplay.top),
      _CropHandle.ne: cropDisplay.topRight,
      _CropHandle.e: Offset(cropDisplay.right, cropDisplay.center.dy),
      _CropHandle.se: cropDisplay.bottomRight,
      _CropHandle.s: Offset(cropDisplay.center.dx, cropDisplay.bottom),
      _CropHandle.sw: cropDisplay.bottomLeft,
      _CropHandle.w: Offset(cropDisplay.left, cropDisplay.center.dy),
    };
  }

  _CropHandle? _hitTestCropHandle(Offset local, Rect cropDisplay) {
    final centers = _handleCenters(cropDisplay);
    _CropHandle? best;
    var bestDistance = double.infinity;
    for (final entry in centers.entries) {
      final distance = (entry.value - local).distance;
      if (distance <= _handleHitSize && distance < bestDistance) {
        best = entry.key;
        bestDistance = distance;
      }
    }
    if (best != null) return best;
    if (cropDisplay.inflate(4).contains(local)) return _CropHandle.body;
    return null;
  }

  Rect _clampCropRect(Rect rect, ui.Image image) {
    final bounds = Rect.fromLTWH(
      0,
      0,
      image.width.toDouble(),
      image.height.toDouble(),
    );
    var left = rect.left.clamp(bounds.left, bounds.right - _minCropSize);
    var top = rect.top.clamp(bounds.top, bounds.bottom - _minCropSize);
    var right = rect.right.clamp(left + _minCropSize, bounds.right);
    var bottom = rect.bottom.clamp(top + _minCropSize, bounds.bottom);
    if (right - left < _minCropSize) {
      right = left + _minCropSize;
    }
    if (bottom - top < _minCropSize) {
      bottom = top + _minCropSize;
    }
    return Rect.fromLTRB(left, top, right, bottom);
  }

  Rect _resizeCropByHandle({
    required Rect start,
    required _CropHandle handle,
    required Offset imagePoint,
    required ui.Image image,
  }) {
    var left = start.left;
    var top = start.top;
    var right = start.right;
    var bottom = start.bottom;
    final x = imagePoint.dx;
    final y = imagePoint.dy;

    switch (handle) {
      case _CropHandle.nw:
        left = x;
        top = y;
      case _CropHandle.n:
        top = y;
      case _CropHandle.ne:
        right = x;
        top = y;
      case _CropHandle.e:
        right = x;
      case _CropHandle.se:
        right = x;
        bottom = y;
      case _CropHandle.s:
        bottom = y;
      case _CropHandle.sw:
        left = x;
        bottom = y;
      case _CropHandle.w:
        left = x;
      case _CropHandle.body:
        break;
    }

    final normalized = Rect.fromLTRB(left, top, right, bottom)._normalized;
    return _clampCropRect(normalized, image);
  }

  Rect _moveCropRect({
    required Rect start,
    required Offset delta,
    required ui.Image image,
  }) {
    final moved = start.shift(delta);
    final maxLeft = image.width - start.width;
    final maxTop = image.height - start.height;
    return Rect.fromLTWH(
      moved.left.clamp(0, math.max(0, maxLeft)),
      moved.top.clamp(0, math.max(0, maxTop)),
      start.width,
      start.height,
    );
  }

  Future<void> _applyCrop() async {
    final image = _image;
    final crop = _cropRect;
    if (image == null || crop == null || _saving) return;
    final bounds = Rect.fromLTWH(
      0,
      0,
      image.width.toDouble(),
      image.height.toDouble(),
    ).intersect(crop);
    if (bounds.width < 8 || bounds.height < 8) return;

    setState(() => _saving = true);
    try {
      final recorder = ui.PictureRecorder();
      final canvas = Canvas(recorder);
      final src = Rect.fromLTWH(
        bounds.left,
        bounds.top,
        bounds.width,
        bounds.height,
      );
      final dst = Rect.fromLTWH(0, 0, bounds.width, bounds.height);
      canvas.drawImageRect(image, src, dst, Paint());
      final picture = recorder.endRecording();
      final cropped = await picture.toImage(
        bounds.width.round().clamp(1, 8192),
        bounds.height.round().clamp(1, 8192),
      );
      picture.dispose();
      if (!mounted) {
        cropped.dispose();
        return;
      }
      setState(() {
        _image?.dispose();
        _image = cropped;
        _cropRect = null;
        _strokes.clear();
        _tool = _EditorTool.brush;
        _saving = false;
      });
    } catch (_) {
      if (mounted) setState(() => _saving = false);
    }
  }

  Future<void> _saveAndPop() async {
    final image = _image;
    if (image == null || _saving) return;
    setState(() => _saving = true);
    try {
      final recorder = ui.PictureRecorder();
      final canvas = Canvas(recorder);
      canvas.drawImage(image, Offset.zero, Paint());
      for (final stroke in _strokes) {
        if (stroke.points.length < 2) continue;
        final paint = Paint()
          ..color = stroke.color
          ..strokeWidth = stroke.width
          ..strokeCap = StrokeCap.round
          ..strokeJoin = StrokeJoin.round
          ..style = PaintingStyle.stroke;
        final path = Path()
          ..moveTo(stroke.points.first.dx, stroke.points.first.dy);
        for (final point in stroke.points.skip(1)) {
          path.lineTo(point.dx, point.dy);
        }
        canvas.drawPath(path, paint);
      }
      final picture = recorder.endRecording();
      final output = await picture.toImage(image.width, image.height);
      picture.dispose();
      final bytes = await output.toByteData(format: ui.ImageByteFormat.png);
      output.dispose();
      if (bytes == null) {
        throw StateError('export_failed');
      }

      final root = await getApplicationDocumentsDirectory();
      final directory =
          Directory(p.join(root.path, 'bandu', 'tutor', 'attachments'));
      await directory.create(recursive: true);
      final target = File(
        p.join(
          directory.path,
          'edited_${DateTime.now().microsecondsSinceEpoch}.png',
        ),
      );
      await target.writeAsBytes(bytes.buffer.asUint8List(), flush: true);
      if (!mounted) return;
      Navigator.of(context).pop(target.path);
    } catch (_) {
      if (!mounted) return;
      setState(() => _saving = false);
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('保存编辑结果失败，请重试')),
      );
    }
  }

  void _onPanStart(DragStartDetails details) {
    final image = _image;
    final viewport = _viewportSize;
    if (image == null || viewport == null) return;
    final point = _toImagePoint(details.localPosition, viewport, image);
    if (point == null) return;

    if (_tool == _EditorTool.brush) {
      final stroke = _Stroke(color: _brushColor, width: _brushWidth);
      stroke.points.add(point);
      setState(() {
        _activeStroke = stroke;
        _strokes.add(stroke);
      });
      return;
    }

    final crop = _ensureCropRect(image);
    final display = _imageDisplayRect(viewport, image);
    final cropDisplay = _cropDisplayRect(crop, display, image);
    final handle = _hitTestCropHandle(details.localPosition, cropDisplay);
    if (handle == null) return;
    setState(() {
      _activeCropHandle = handle;
      _cropRectAtDragStart = crop;
      _cropDragStartImagePoint = point;
      _cropRect = crop;
    });
  }

  void _onPanUpdate(DragUpdateDetails details) {
    final image = _image;
    final viewport = _viewportSize;
    if (image == null || viewport == null) return;
    final point = _toImagePoint(details.localPosition, viewport, image);
    if (point == null) return;

    if (_tool == _EditorTool.brush && _activeStroke != null) {
      setState(() => _activeStroke!.points.add(point));
      return;
    }

    final handle = _activeCropHandle;
    final startRect = _cropRectAtDragStart;
    final startPoint = _cropDragStartImagePoint;
    if (handle == null || startRect == null || startPoint == null) return;

    setState(() {
      if (handle == _CropHandle.body) {
        _cropRect = _moveCropRect(
          start: startRect,
          delta: point - startPoint,
          image: image,
        );
      } else {
        _cropRect = _resizeCropByHandle(
          start: startRect,
          handle: handle,
          imagePoint: point,
          image: image,
        );
      }
    });
  }

  void _onPanEnd(DragEndDetails details) {
    _activeStroke = null;
    _activeCropHandle = null;
    _cropRectAtDragStart = null;
    _cropDragStartImagePoint = null;
  }

  @override
  Widget build(BuildContext context) {
    final colorScheme = Theme.of(context).colorScheme;
    final image = _image;
    return Scaffold(
      backgroundColor: Colors.black,
      appBar: AppBar(
        backgroundColor: Colors.black,
        foregroundColor: Colors.white,
        title: const Text('编辑图片'),
        leading: IconButton(
          tooltip: '取消',
          onPressed: _saving ? null : () => Navigator.of(context).pop(),
          icon: const Icon(Icons.close),
        ),
        actions: [
          TextButton(
            onPressed: _saving || image == null ? null : _saveAndPop,
            child: _saving
                ? const SizedBox(
                    width: 18,
                    height: 18,
                    child: CircularProgressIndicator(strokeWidth: 2),
                  )
                : const Text('完成'),
          ),
        ],
      ),
      body: Column(
        children: [
          Expanded(
            child: _loadError != null
                ? Center(
                    child: Text(
                      _loadError!,
                      style: const TextStyle(color: Colors.white70),
                    ),
                  )
                : image == null
                    ? const Center(child: CircularProgressIndicator())
                    : LayoutBuilder(
                        builder: (context, constraints) {
                          final viewport = Size(
                            constraints.maxWidth,
                            constraints.maxHeight,
                          );
                          _viewportSize = viewport;
                          final display = _imageDisplayRect(viewport, image);
                          final crop = _tool == _EditorTool.crop
                              ? (_cropRect ?? _defaultCropRect(image))
                              : null;
                          final cropDisplay = crop == null
                              ? null
                              : _cropDisplayRect(crop, display, image);
                          return GestureDetector(
                            behavior: HitTestBehavior.opaque,
                            onPanStart: _onPanStart,
                            onPanUpdate: _onPanUpdate,
                            onPanEnd: _onPanEnd,
                            child: Stack(
                              fit: StackFit.expand,
                              children: [
                                CustomPaint(
                                  painter: _EditorPainter(
                                    image: image,
                                    displayRect: display,
                                    strokes: _strokes,
                                    cropRect: crop,
                                  ),
                                ),
                                if (cropDisplay != null)
                                  _CropHandlesOverlay(
                                    cropDisplay: cropDisplay,
                                    handleColor: colorScheme.primary,
                                  ),
                              ],
                            ),
                          );
                        },
                      ),
          ),
          Material(
            color: const Color(0xFF1C1C1E),
            child: SafeArea(
              top: false,
              child: Padding(
                padding: const EdgeInsets.fromLTRB(
                  AppSpacing.medium,
                  AppSpacing.small,
                  AppSpacing.medium,
                  AppSpacing.medium,
                ),
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    SegmentedButton<_EditorTool>(
                      style: ButtonStyle(
                        backgroundColor:
                            WidgetStateProperty.resolveWith((states) {
                          if (states.contains(WidgetState.selected)) {
                            return colorScheme.primary;
                          }
                          return const Color(0xFF5C5C5E);
                        }),
                        foregroundColor:
                            WidgetStateProperty.resolveWith((states) {
                          if (states.contains(WidgetState.selected)) {
                            return colorScheme.onPrimary;
                          }
                          return const Color(0xFFE5E5EA);
                        }),
                        side: const WidgetStatePropertyAll(BorderSide.none),
                      ),
                      segments: const [
                        ButtonSegment(
                          value: _EditorTool.crop,
                          label: Text('裁剪'),
                          icon: Icon(Icons.crop),
                        ),
                        ButtonSegment(
                          value: _EditorTool.brush,
                          label: Text('画笔'),
                          icon: Icon(Icons.brush_outlined),
                        ),
                      ],
                      selected: {_tool},
                      onSelectionChanged: _saving
                          ? null
                          : (selection) {
                              setState(() {
                                _tool = selection.first;
                                if (_tool == _EditorTool.crop &&
                                    _image != null) {
                                  _cropRect ??= _defaultCropRect(_image!);
                                }
                              });
                            },
                    ),
                    const SizedBox(height: AppSpacing.medium),
                    if (_tool == _EditorTool.brush) ...[
                      Row(
                        children: [
                          for (final color in _brushColors)
                            GestureDetector(
                              onTap: () => setState(() => _brushColor = color),
                              child: Container(
                                width: 28,
                                height: 28,
                                margin: const EdgeInsets.only(
                                  right: AppSpacing.small,
                                ),
                                decoration: BoxDecoration(
                                  color: color,
                                  shape: BoxShape.circle,
                                  border: Border.all(
                                    color: _brushColor == color
                                        ? colorScheme.primary
                                        : Colors.white24,
                                    width: _brushColor == color ? 2.5 : 1,
                                  ),
                                ),
                              ),
                            ),
                          const Spacer(),
                          IconButton(
                            tooltip: '撤销',
                            onPressed: _strokes.isEmpty
                                ? null
                                : () => setState(() => _strokes.removeLast()),
                            icon: const Icon(Icons.undo, color: Colors.white),
                          ),
                        ],
                      ),
                      Row(
                        children: [
                          const Text(
                            '粗细',
                            style: TextStyle(color: Colors.white70),
                          ),
                          Expanded(
                            child: Slider(
                              value: _brushWidth,
                              min: 2,
                              max: 16,
                              onChanged: (value) =>
                                  setState(() => _brushWidth = value),
                            ),
                          ),
                        ],
                      ),
                    ] else ...[
                      Text(
                        '拖动四角或四边控制点调整裁剪框，拖动框内可移动，然后点“应用裁剪”',
                        style: Theme.of(context).textTheme.bodySmall?.copyWith(
                              color: Colors.white70,
                            ),
                      ),
                      const SizedBox(height: AppSpacing.small),
                      SizedBox(
                        width: double.infinity,
                        child: FilledButton.icon(
                          onPressed: _saving ? null : _applyCrop,
                          icon: const Icon(Icons.check),
                          label: const Text('应用裁剪'),
                        ),
                      ),
                    ],
                  ],
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}

extension on Rect {
  Rect get _normalized {
    return Rect.fromLTRB(
      math.min(left, right),
      math.min(top, bottom),
      math.max(left, right),
      math.max(top, bottom),
    );
  }
}

class _CropHandlesOverlay extends StatelessWidget {
  const _CropHandlesOverlay({
    required this.cropDisplay,
    required this.handleColor,
  });

  final Rect cropDisplay;
  final Color handleColor;

  static const _handleSize = 14.0;

  @override
  Widget build(BuildContext context) {
    final centers = <Offset>[
      cropDisplay.topLeft,
      Offset(cropDisplay.center.dx, cropDisplay.top),
      cropDisplay.topRight,
      Offset(cropDisplay.right, cropDisplay.center.dy),
      cropDisplay.bottomRight,
      Offset(cropDisplay.center.dx, cropDisplay.bottom),
      cropDisplay.bottomLeft,
      Offset(cropDisplay.left, cropDisplay.center.dy),
    ];
    return Stack(
      children: [
        Positioned.fromRect(
          rect: cropDisplay,
          child: IgnorePointer(
            child: DecoratedBox(
              decoration: BoxDecoration(
                border: Border.all(color: handleColor, width: 2),
              ),
            ),
          ),
        ),
        for (final center in centers)
          Positioned(
            left: center.dx - _handleSize / 2,
            top: center.dy - _handleSize / 2,
            width: _handleSize,
            height: _handleSize,
            child: IgnorePointer(
              child: DecoratedBox(
                decoration: BoxDecoration(
                  color: Colors.white,
                  borderRadius: BorderRadius.circular(2),
                  border: Border.all(color: handleColor, width: 1.5),
                  boxShadow: const [
                    BoxShadow(
                      color: Colors.black54,
                      blurRadius: 2,
                      offset: Offset(0, 1),
                    ),
                  ],
                ),
              ),
            ),
          ),
      ],
    );
  }
}

class _EditorPainter extends CustomPainter {
  const _EditorPainter({
    required this.image,
    required this.displayRect,
    required this.strokes,
    required this.cropRect,
  });

  final ui.Image image;
  final Rect displayRect;
  final List<_Stroke> strokes;
  final Rect? cropRect;

  @override
  void paint(Canvas canvas, Size size) {
    canvas.drawRect(Offset.zero & size, Paint()..color = Colors.black);
    final src = Rect.fromLTWH(
      0,
      0,
      image.width.toDouble(),
      image.height.toDouble(),
    );
    canvas.drawImageRect(image, src, displayRect, Paint());

    if (cropRect != null) {
      final cropDisplay = Rect.fromLTWH(
        displayRect.left + cropRect!.left / image.width * displayRect.width,
        displayRect.top + cropRect!.top / image.height * displayRect.height,
        cropRect!.width / image.width * displayRect.width,
        cropRect!.height / image.height * displayRect.height,
      );
      final dim = Path()
        ..addRect(Offset.zero & size)
        ..addRect(cropDisplay)
        ..fillType = PathFillType.evenOdd;
      canvas.drawPath(dim, Paint()..color = Colors.black54);
    }

    canvas.save();
    canvas.translate(displayRect.left, displayRect.top);
    canvas.scale(
      displayRect.width / image.width,
      displayRect.height / image.height,
    );
    for (final stroke in strokes) {
      if (stroke.points.length < 2) continue;
      final paint = Paint()
        ..color = stroke.color
        ..strokeWidth = stroke.width
        ..strokeCap = StrokeCap.round
        ..strokeJoin = StrokeJoin.round
        ..style = PaintingStyle.stroke;
      final path = Path()..moveTo(stroke.points.first.dx, stroke.points.first.dy);
      for (final point in stroke.points.skip(1)) {
        path.lineTo(point.dx, point.dy);
      }
      canvas.drawPath(path, paint);
    }
    canvas.restore();
  }

  @override
  bool shouldRepaint(covariant _EditorPainter oldDelegate) => true;
}
