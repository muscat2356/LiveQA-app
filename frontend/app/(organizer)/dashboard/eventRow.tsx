"use client";

import { Badge } from "@/components/ui/badge";
import { Card, CardContent } from "@/components/ui/card";
import {
  EventListResponse,
  statusLabel,
  statusVariant,
} from "@/types/event/event";


import Link from "next/link";
import StatusChangeDialog from "@/components/event/statusChangeDialog";

export default function EventRow({ event }: {event: EventListResponse }) {

    const status = event.status;

    return(
        <Card>
            <CardContent>
                <div>
                    <Link href={`/dashboard/${event.id}`}>
                        {event.title}
                    </Link>
                    <p>
                        参加コード： {event.joinCode}
                    </p>
                    <p>
                        作成日時： {event.createdAt}
                    </p>
                </div>

                <div>
                    <Badge variant={statusVariant[status]}>{statusLabel[status]}</Badge>
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
