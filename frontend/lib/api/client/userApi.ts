import { apiClient } from "../../http/apiClient";

/**
 * ユーザー登録API　クライアント用
 * @param email 
 */
export async function fetchPostUserRegister( email: string ): Promise<void>{
    
    return apiClient<void>(`/api/auth/register`,{
        method:"POST",
        body: {email },
    });
}