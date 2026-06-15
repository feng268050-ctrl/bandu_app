(() => {
  'use strict';

  const markdown = window.markdownit({
    html: false,
    linkify: false,
    typographer: false,
    breaks: true,
  });
  markdown.enable('table');

  markdown.renderer.rules.image = () => '';
  markdown.renderer.rules.link_open = () => '';
  markdown.renderer.rules.link_close = () => '';

  const renderMath = (source, displayMode) => window.katex.renderToString(
    source,
    {
      displayMode,
      throwOnError: false,
      strict: 'ignore',
      trust: false,
      output: 'htmlAndMathml',
    },
  );

  const mathInline = (state, silent) => {
    const start = state.pos;
    if (state.src[start] !== '$' || state.src[start + 1] === '$') {
      return false;
    }

    let end = start + 1;
    while ((end = state.src.indexOf('$', end)) !== -1) {
      if (state.src[end - 1] !== '\\') {
        break;
      }
      end += 1;
    }
    if (end === -1 || end === start + 1) {
      return false;
    }

    if (!silent) {
      const token = state.push('math_inline', 'math', 0);
      token.content = state.src.slice(start + 1, end);
      token.markup = '$';
    }
    state.pos = end + 1;
    return true;
  };

  const mathBlock = (state, startLine, endLine, silent) => {
    const start = state.bMarks[startLine] + state.tShift[startLine];
    const finish = state.eMarks[startLine];
    const opening = state.src.slice(start, finish).trim();
    if (!opening.startsWith('$$')) {
      return false;
    }
    if (silent) {
      return true;
    }

    const lines = [];
    const inlineClosing = opening.slice(2).indexOf('$$');
    let nextLine = startLine;
    if (inlineClosing >= 0) {
      lines.push(opening.slice(2, inlineClosing + 2));
    } else {
      const openingRemainder = opening.slice(2);
      if (openingRemainder) {
        lines.push(openingRemainder);
      }
      for (nextLine = startLine + 1; nextLine < endLine; nextLine += 1) {
        const lineStart = state.bMarks[nextLine] + state.tShift[nextLine];
        const lineEnd = state.eMarks[nextLine];
        const line = state.src.slice(lineStart, lineEnd);
        const closing = line.indexOf('$$');
        if (closing >= 0) {
          lines.push(line.slice(0, closing));
          break;
        }
        lines.push(line);
      }
      if (nextLine >= endLine) {
        return false;
      }
    }

    state.line = nextLine + 1;
    const token = state.push('math_block', 'math', 0);
    token.block = true;
    token.content = lines.join('\n').trim();
    token.map = [startLine, state.line];
    token.markup = '$$';
    return true;
  };

  markdown.inline.ruler.after('escape', 'math_inline', mathInline);
  markdown.block.ruler.after(
    'blockquote',
    'math_block',
    mathBlock,
    { alt: ['paragraph', 'reference', 'blockquote', 'list'] },
  );
  markdown.renderer.rules.math_inline = (tokens, index) => (
    renderMath(tokens[index].content, false)
  );
  markdown.renderer.rules.math_block = (tokens, index) => (
    `<div class="math-block">${renderMath(tokens[index].content, true)}</div>`
  );

  const sanitizerConfig = Object.freeze({
    USE_PROFILES: { html: true, mathMl: true },
    FORBID_TAGS: [
      'a',
      'audio',
      'button',
      'embed',
      'form',
      'frame',
      'iframe',
      'img',
      'input',
      'object',
      'option',
      'picture',
      'script',
      'select',
      'source',
      'style',
      'textarea',
      'video',
    ],
    FORBID_ATTR: [
      'action',
      'formaction',
      'src',
      'srcset',
      'xlink:href',
    ],
  });

  window.renderMarkdown = (source) => {
    const rendered = markdown.render(typeof source === 'string' ? source : '');
    const clean = window.DOMPurify.sanitize(rendered, sanitizerConfig);
    document.getElementById('content').replaceChildren(
      document.createRange().createContextualFragment(clean),
    );
  };

  document.addEventListener('click', (event) => {
    event.preventDefault();
    event.stopPropagation();
  }, true);
})();
