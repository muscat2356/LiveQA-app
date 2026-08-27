import { cn } from "@/lib/utils";

const COLORS = [
    "#2ba8dd",
    "#6aa84f",
    "#e0a94b",
    "#c9679e",
    "#8e7cc3",
    "#5b8def",
];

/**
 * 投稿者名の頭文字を表示する丸アバター
 * 色は名前のハッシュから決まる（同じ名前は常に同じ色）
 */
export function Avatar({
    name,
    size = 24,
    className,
}: {
    name: string;
    size?: number;
    className?: string;
}) {
    const trimmed = name.trim();
    const initial = trimmed.charAt(0) || "?";

    let hash = 0;
    for (let i = 0; i < trimmed.length; i++) {
        hash = (hash * 31 + trimmed.charCodeAt(i)) >>> 0;
    }
    const background = COLORS[hash % COLORS.length];

    return (
        <span
            aria-hidden
            className={cn(
                "inline-flex shrink-0 items-center justify-center rounded-full font-bold text-white",
                className,
            )}
            style={{
                width: size,
                height: size,
                fontSize: size * 0.45,
                background,
            }}
        >
            {initial}
        </span>
    );
}
