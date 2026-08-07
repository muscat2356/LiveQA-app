import { Card, CardContent } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";

export default function Loading() {
    return (
        <div>
            <Skeleton className="h-8 w-48" />
            {Array.from({ length: 3 }).map((_, i) => (
                <Card key={i}>
                    <CardContent>
                        <Skeleton className="h-5 w-64" />
                        <Skeleton className="h-4 w-32" />
                        <Skeleton className="h-4 w-40" />
                    </CardContent>
                </Card>
            ))}
        </div>
    );
}