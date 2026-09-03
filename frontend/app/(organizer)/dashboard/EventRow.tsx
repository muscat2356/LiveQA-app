import { Badge } from "@/components/ui/badge";import { Card, CardContent } from "@/components/ui/card";
import {
  EventListResponse,
  EventStatus,
  statusLabel,
} from "@/types/event/event";


import Link from "next/link";
import StatusChangeDialog from "@/components/event/StatusChangeDialog";
import CopyButton from "@/components/event/CopyButton";
import formDateTime from "@/lib/utils/date";
import { RiTimeLine } from "@remixicon/react";

// ステータスごとの表示色（色分けのみ）
const statusBadgeClass: Record<EventStatus, string> = {
  DRAFT: "bg-amber-100 text-amber-800 dark:bg-amber-500/15 dark:text-amber-300",
  OPEN: "bg-green-100 text-green-800 dark:bg-green-500/15 dark:text-green-300",
  CLOSED: "bg-muted text-muted-foreground",
};

export default function EventRow({ event }: {event: EventListResponse }) {

    const status = event.status;
    const isOpen = status === "OPEN";

    return(
        <Card className={`transition-shadow hover:shadow-lg ${isOpen ? "ring-1 ring-primary/40" : ""}`}>
            <CardContent className="space-y-4">
                {/* 見出し */}
                <div className="flex items-start justify-between gap-3">
                    <Link
                        href={`/dashboard/${event.id}`}
                        className="flex min-w-0 items-center gap-2 text-base font-semibold hover:text-primary hover:underline"
                    >
                        {isOpen && (
                            <span
                                aria-hidden
                                className="size-2 shrink-0 rounded-full bg-green-500 ring-4 ring-green-500/20"
                            />
                        )}
                        <span className="truncate">{event.title}</span>
                    </Link>
                    <Badge variant="secondary" className={`shrink-0 ${statusBadgeClass[status]}`}>{statusLabel[status]}</Badge>
                </div>

                {/* 参加コード・作成日時 */}
                <div className="flex flex-wrap items-center gap-x-4 gap-y-2 text-sm text-muted-foreground">
                    <span className="inline-flex items-center gap-2">
                        <span className="rounded-md bg-muted px-2 py-1 font-mono font-semibold tracking-wider text-foreground">{event.joinCode}</span>
                        <CopyButton text={event.joinCode} />
                    </span>
                    <span className="inline-flex items-center gap-1">
                        <RiTimeLine className="size-4" />
                        {formDateTime(event.createdAt)}
                    </span>
                </div>

                {/* 操作 */}
                <div className="flex items-center justify-between gap-3 border-t pt-3">
                    <Link
                        href={`/dashboard/${event.id}`}
                        className="text-sm text-muted-foreground transition-colors hover:text-foreground"
                    >
                        管理画面へ →
                    </Link>
                    <StatusChangeDialog
                        eventId={event.id}
                        title={event.title}
                        status={event.status}
                    />
                </div>
            </CardContent>
        </Card>
    );
}
