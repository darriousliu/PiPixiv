import { Fragment, type ReactNode } from "react";

/** 用对应的值替换 `{name}` 占位符。 */
export function fill(template: string, values: Record<string, string | number>): string {
  return template.replace(/\{(\w+)\}/g, (match, key: string) =>
    key in values ? String(values[key]) : match,
  );
}

/** 与 `fill` 相同，但占位符可以替换为 React 节点。 */
export function fillNodes(template: string, values: Record<string, ReactNode>): ReactNode[] {
  return template.split(/(\{\w+\})/g).map((part, i) => {
    const key = /^\{(\w+)\}$/.exec(part)?.[1];
    return <Fragment key={i}>{key && key in values ? values[key] : part}</Fragment>;
  });
}

/** Renders `[label](https://…)` in dictionary strings as external links. */
export function richText(text: string): ReactNode[] {
  return text.split(/(\[[^\]]+\]\([^)]+\))/g).map((part, i) => {
    const link = /^\[([^\]]+)\]\(([^)]+)\)$/.exec(part);
    if (!link) return <Fragment key={i}>{part}</Fragment>;
    return (
      <a key={i} href={link[2]} target="_blank" rel="noopener">
        {link[1]}
      </a>
    );
  });
}

export function formatBytes(bytes: number): string {
  return `${Math.round(bytes / 1048576)} MB`;
}
