"use client";

export default function Error({ reset }: { error: Error; reset: () => void }) {
    return (
        <div>
            <p>イベントの取得に失敗しました。</p>
            <button onClick={reset}>再試行</button>
        </div>
    );
}