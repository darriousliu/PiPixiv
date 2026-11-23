/**
 * Material Symbols Rounded 连字图标。
 * 字体是子集，使用新的 `name` 后需要重新生成（见 README）。
 */
export function Icon({ name, className }: { name: string; className?: string }) {
  return (
    <span className={className ? `ms ${className}` : "ms"} aria-hidden="true">
      {name}
    </span>
  );
}
