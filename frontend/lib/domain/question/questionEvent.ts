import { QuestionEvent, QuestionFilter, QuestionResponse, Sort } from "@/types/question/question";

export function sortQuestions(
    questionList:QuestionResponse[], 
    sort: Sort,
): 
    QuestionResponse[] 
{

    return [...questionList].sort((first, second) => {
        if (sort === "popular") {
            return second.upvoteCount - first.upvoteCount;
        }
     return Date.parse(second.createdAt)
            - Date.parse(first.createdAt);
    });
}

export function receiveEvent(
    previousQuestions: QuestionResponse[],
    questionEvent: QuestionEvent,
    filter: QuestionFilter,
    sort: Sort,
    includeArchived: boolean,
):
    QuestionResponse[]
{
    //質問が存在するのか確認
    const questionExits = previousQuestions.some(
        question => 
            question.id === questionEvent.question.id
    );

    //filterの合致確認用変数
    const matchesFilter = 
        (
        questionEvent.question.status !== "ARCHIVED"
        || includeArchived
    ) && (
        filter === "ALL"
        || questionEvent.question.status === filter
    );

    if(questionExits && !matchesFilter){
        return [...previousQuestions].filter((q) => q.id !== questionEvent.question.id);
    }

    if(questionExits && matchesFilter){
        return sortQuestions(previousQuestions.map((q) =>
            q.id === questionEvent.question.id 
        ? {...q, ...questionEvent.question}
        : q
        ), sort);
    }

    if(!questionExits && matchesFilter && questionEvent.question.status !== "ARCHIVED"){
        return sortQuestions(
            [{ ...questionEvent.question, votedByMe: false }, 
            ...previousQuestions]
            ,sort);
    }

    return previousQuestions;
}