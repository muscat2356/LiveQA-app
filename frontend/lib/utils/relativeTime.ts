/**
 * ISO日時を「3分前」などの相対表記に変換する
 * 7日以上前は月/日にフォールバックする
 * @param iso ISO形式の日時文字列
 * @returns 相対表記の文字列
 */
export default function relativeTime(iso: string): string {
    const then = new Date(iso).getTime();
    if (Number.isNaN(then)) return "";

    const sec = Math.floor((Date.now() - then) / 1000);
    if (sec < 0) return "たった今";
    if (sec < 60) return "たった今";

    const min = Math.floor(sec / 60);
    if (min < 60) return `${min}分前`;

    const hr = Math.floor(min / 60);
    if (hr < 24) return `${hr}時間前`;

    const day = Math.floor(hr / 24);
    if (day < 7) return `${day}日前`;

    return new Intl.DateTimeFormat("ja-JP", {
        month: "2-digit",
        day: "2-digit",
    }).format(new Date(then));
}
