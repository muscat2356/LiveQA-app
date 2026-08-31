import { ApiError } from "./apiError";

/**
 * ServerActions/ServerComponent用
 * 取得できない場合のpage側の分岐に活用
 * 該当ステータスコードの場合にnullをリターン
 * @param request 
 * @param statuses 
 * @returns 
 */
export async function orNull<T>(
    request: Promise<T>,
    statuses: number[]
): Promise<T | null>{

    try{
        return await request;
    }catch(e){
        if(e instanceof ApiError && statuses.includes(e.status)){
            return null
        }
        throw e;
    }
}