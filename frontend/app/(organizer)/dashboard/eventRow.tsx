"use client";

import { Card, CardContent } from "@/components/ui/card";
import { EventListResponse, EventStatus } from "@/types/event/event";
import Link from "next/link";
import { useState } from "react";

export default function EventRow({ event }: {event: EventListResponse }) {

    const [ status, setStatus ] = useState<EventStatus>(event.status);
    const [ open, setOpen ] = useState(false);

    const nextStatus: Exclude<EventStatus, "DRAFT"> 
        = status === "DRAFT" ? "OPEN" : "CLOSED";

    const actionLabel = status === "DRAFT" ? "公開する" : "終了する";

    function handleConfirm(){
        setStatus(nextStatus);
        setOpen(false);
    }

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
                </div>

                <div>
                    <Badge variant={statusVariant[status]}></Badge>
                </div>
            </CardContent>
        </Card>
    );  
}