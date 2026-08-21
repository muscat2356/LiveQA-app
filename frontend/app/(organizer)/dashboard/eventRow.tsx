"use client";

import { Badge } from "@/components/ui/badge";
import { Card, CardContent } from "@/components/ui/card";
import {
  EventListResponse,
  EventStatus,
  statusLabel,
} from "@/types/event/event";


import Link from "next/link";
import StatusChangeDialog from "@/components/event/statusChangeDialog";

// ステータスごとの表示色（色分けのみ）
const statusBadgeClass: Record<EventStatus, string> = {
  DRAFT: "bg-amber-100 text-amber-800 dark:bg-amber-500/15 dark:text-amber-300",
  OPEN: "bg-green-100 text-green-800 dark:bg-green-500/15 dark:text-green-300",
  CLOSED: "bg-muted text-muted-foreground",
};

export default function EventRow({ event }: {event: EventListResponse }) {

    const status = event.status;

    return(
        <Card className="transition-shadow hover:shadow-lg">
            <CardContent className="flex items-start justify-between gap-4">
                <div className="min-w-0 space-y-1">
                    <Link
                        href={`/dashboard/${event.id}`}
                        className="block truncate text-base font-semibold hover:text-primary hover:underline"
                    >
                        {event.title}
                    </Link>
                    <p className="text-sm text-muted-foreground">
                        参加コード： <span className="font-mono font-medium text-foreground">{event.joinCode}</span>
                    </p>
                    <p className="text-sm text-muted-foreground">
                        作成日時： {event.createdAt}
                    </p>
                </div>

                <div className="flex shrink-0 flex-col items-end gap-2">
                    <Badge variant="secondary" className={statusBadgeClass[status]}>{statusLabel[status]}</Badge>
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
