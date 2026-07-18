import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

class AppNumberStepper extends StatefulWidget {
  const AppNumberStepper({
    required this.label,
    required this.supportingText,
    required this.value,
    required this.minimum,
    required this.maximum,
    required this.onChanged,
    this.enabled = true,
    this.decrementKey,
    this.valueKey,
    this.incrementKey,
    super.key,
  });

  final String label;
  final String supportingText;
  final int value;
  final int minimum;
  final int maximum;
  final ValueChanged<int> onChanged;
  final bool enabled;
  final Key? decrementKey;
  final Key? valueKey;
  final Key? incrementKey;

  @override
  State<AppNumberStepper> createState() => _AppNumberStepperState();
}

class _AppNumberStepperState extends State<AppNumberStepper> {
  late final TextEditingController _controller;
  late final FocusNode _focusNode;

  @override
  void initState() {
    super.initState();
    _controller = TextEditingController(text: '${widget.value}');
    _focusNode = FocusNode()..addListener(_handleFocusChanged);
  }

  @override
  void didUpdateWidget(AppNumberStepper oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (!_focusNode.hasFocus && widget.value != oldWidget.value) {
      _setText(widget.value);
    }
  }

  @override
  void dispose() {
    _focusNode
      ..removeListener(_handleFocusChanged)
      ..dispose();
    _controller.dispose();
    super.dispose();
  }

  void _handleFocusChanged() {
    if (_focusNode.hasFocus) {
      _controller.selection = TextSelection(
        baseOffset: 0,
        extentOffset: _controller.text.length,
      );
    } else {
      _commitInput();
    }
  }

  void _commitInput() {
    final parsed = int.tryParse(_controller.text);
    final normalized = (parsed ?? widget.minimum).clamp(
      widget.minimum,
      widget.maximum,
    );
    _setText(normalized);
    if (normalized != widget.value) {
      widget.onChanged(normalized);
    }
  }

  void _setText(int value) {
    final text = '$value';
    _controller.value = TextEditingValue(
      text: text,
      selection: TextSelection.collapsed(offset: text.length),
    );
  }

  @override
  Widget build(BuildContext context) {
    return ListTile(
      contentPadding: EdgeInsets.zero,
      title: Text(widget.label),
      subtitle: Text(widget.supportingText),
      trailing: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          IconButton(
            key: widget.decrementKey,
            tooltip: '减少${widget.label}',
            onPressed: widget.enabled && widget.value > widget.minimum
                ? () => widget.onChanged(widget.value - 1)
                : null,
            icon: const Icon(Icons.remove_circle_outline),
          ),
          SizedBox(
            width: 52,
            child: TextField(
              key: widget.valueKey,
              controller: _controller,
              focusNode: _focusNode,
              enabled: widget.enabled,
              keyboardType: TextInputType.number,
              textInputAction: TextInputAction.done,
              textAlign: TextAlign.center,
              maxLength: widget.maximum.toString().length,
              inputFormatters: [FilteringTextInputFormatter.digitsOnly],
              decoration: const InputDecoration(
                isDense: true,
                counterText: '',
                contentPadding: EdgeInsets.symmetric(vertical: 10),
              ),
              style: Theme.of(context).textTheme.titleMedium,
              onSubmitted: (_) => _focusNode.unfocus(),
              onTapOutside: (_) => _focusNode.unfocus(),
            ),
          ),
          IconButton(
            key: widget.incrementKey,
            tooltip: '增加${widget.label}',
            onPressed: widget.enabled && widget.value < widget.maximum
                ? () => widget.onChanged(widget.value + 1)
                : null,
            icon: const Icon(Icons.add_circle_outline),
          ),
        ],
      ),
    );
  }
}
