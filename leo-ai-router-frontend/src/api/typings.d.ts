declare namespace API {
  type ApiKeyCreateRequest = {
    keyName?: string
  }

  type ApiKeyVO = {
    id?: number
    keyValue?: string
    keyName?: string
    status?: string
    totalTokens?: number
    lastUsedTime?: string
    createTime?: string
  }

  type BalanceVO = {
    balance?: number
    totalSpending?: number
    totalRecharge?: number
  }

  type BaseResponseApiKeyVO = {
    code?: number
    data?: ApiKeyVO
    message?: string
  }

  type BaseResponseBalanceVO = {
    code?: number
    data?: BalanceVO
    message?: string
  }

  type BaseResponseBoolean = {
    code?: number
    data?: boolean
    message?: string
  }

  type BaseResponseCostStatsVO = {
    code?: number
    data?: CostStatsVO
    message?: string
  }

  type BaseResponseCreateRechargeResponse = {
    code?: number
    data?: CreateRechargeResponse
    message?: string
  }

  type BaseResponseListMapStringObject = {
    code?: number
    data?: Record<string, any>[]
    message?: string
  }

  type BaseResponseLoginUserVO = {
    code?: number
    data?: LoginUserVO
    message?: string
  }

  type BaseResponseLong = {
    code?: number
    data?: number
    message?: string
  }

  type BaseResponsePageApiKeyVO = {
    code?: number
    data?: PageApiKeyVO
    message?: string
  }

  type BaseResponsePageBillingRecord = {
    code?: number
    data?: PageBillingRecord
    message?: string
  }

  type BaseResponsePageRechargeRecord = {
    code?: number
    data?: PageRechargeRecord
    message?: string
  }

  type BaseResponsePageRequestLog = {
    code?: number
    data?: PageRequestLog
    message?: string
  }

  type BaseResponsePageUserVO = {
    code?: number
    data?: PageUserVO
    message?: string
  }

  type BaseResponseQuotaVO = {
    code?: number
    data?: QuotaVO
    message?: string
  }

  type BaseResponseRequestLog = {
    code?: number
    data?: RequestLog
    message?: string
  }

  type BaseResponseSetString = {
    code?: number
    data?: string[]
    message?: string
  }

  type BaseResponseString = {
    code?: number
    data?: string
    message?: string
  }

  type BaseResponseUser = {
    code?: number
    data?: User
    message?: string
  }

  type BaseResponseUserAnalysisVO = {
    code?: number
    data?: UserAnalysisVO
    message?: string
  }

  type BaseResponseUserSummaryStatsVO = {
    code?: number
    data?: UserSummaryStatsVO
    message?: string
  }

  type BaseResponseUserVO = {
    code?: number
    data?: UserVO
    message?: string
  }

  type BillingRecord = {
    id?: number
    userId?: number
    requestLogId?: number
    amount?: number
    balanceBefore?: number
    balanceAfter?: number
    description?: string
    billingType?: string
    createTime?: string
  }

  type BlacklistRequest = {
    ip?: string
    reason?: string
  }

  type ChatMessage = {
    role?: string
    content?: string
  }

  type ChatRequest = {
    model?: string
    messages?: ChatMessage[]
    stream?: boolean
    temperature?: number
    max_tokens?: number
    enable_reasoning?: boolean
    routing_strategy?: string
    plugin_key?: string
    file_url?: string
    file_type?: string
  }

  type checkBlacklistParams = {
    ip: string
  }

  type CostStatsVO = {
    totalCost?: number
    todayCost?: number
  }

  type CreateRechargeRequest = {
    amount?: number
  }

  type CreateRechargeResponse = {
    checkoutUrl?: string
    sessionId?: string
  }

  type DeleteRequest = {
    id?: number
  }

  type disableUserParams = {
    userId: number
  }

  type enableUserParams = {
    userId: number
  }

  type getHistoryDetailParams = {
    id: number
  }

  type getMyBillingRecordsParams = {
    pageNum?: number
    pageSize?: number
  }

  type getMyDailyStatsParams = {
    startDate?: string
    endDate?: string
  }

  type getMyRechargeRecordsParams = {
    pageNum?: number
    pageSize?: number
  }

  type getUserAnalysisParams = {
    userId: number
  }

  type getUserByIdParams = {
    id: number
  }

  type getUserVOByIdParams = {
    id: number
  }

  type listMyApiKeysParams = {
    pageNum?: number
    pageSize?: number
  }

  type LoginUserVO = {
    id?: number
    userAccount?: string
    userName?: string
    userEmail?: string
    userAvatar?: string
    userProfile?: string
    userRole?: string
    quota?: number
    vipTime?: string
    createTime?: string
    updateTime?: string
    tokenQuota?: number
    usedTokens?: number
    userStatus?: string
    balance?: number
  }

  type PageApiKeyVO = {
    records?: ApiKeyVO[]
    pageNumber?: number
    pageSize?: number
    totalPage?: number
    totalRow?: number
    optimizeCountQuery?: boolean
  }

  type PageBillingRecord = {
    records?: BillingRecord[]
    pageNumber?: number
    pageSize?: number
    totalPage?: number
    totalRow?: number
    optimizeCountQuery?: boolean
  }

  type PageRechargeRecord = {
    records?: RechargeRecord[]
    pageNumber?: number
    pageSize?: number
    totalPage?: number
    totalRow?: number
    optimizeCountQuery?: boolean
  }

  type PageRequestLog = {
    records?: RequestLog[]
    pageNumber?: number
    pageSize?: number
    totalPage?: number
    totalRow?: number
    optimizeCountQuery?: boolean
  }

  type PageUserVO = {
    records?: UserVO[]
    pageNumber?: number
    pageSize?: number
    totalPage?: number
    totalRow?: number
    optimizeCountQuery?: boolean
  }

  type QuotaUpdateRequest = {
    userId?: number
    tokenQuota?: number
  }

  type QuotaVO = {
    tokenQuota?: number
    usedTokens?: number
    remainingQuota?: number
  }

  type RechargeRecord = {
    id?: number
    userId?: number
    amount?: number
    paymentMethod?: string
    paymentId?: string
    status?: string
    description?: string
    createTime?: string
    updateTime?: string
  }

  type RequestLog = {
    id?: number
    traceId?: string
    userId?: number
    apiKeyId?: number
    modelId?: number
    requestModel?: string
    modelName?: string
    requestType?: string
    source?: string
    promptTokens?: number
    completionTokens?: number
    totalTokens?: number
    duration?: number
    status?: string
    errorMessage?: string
    errorCode?: string
    routingStrategy?: string
    isFallback?: number
    clientIp?: string
    userAgent?: string
    createTime?: string
    updateTime?: string
    cost?: number
  }

  type RequestLogQueryRequest = {
    pageNum?: number
    pageSize?: number
    sortField?: string
    sortOrder?: string
    userId?: number
    requestModel?: string
    requestType?: string
    source?: string
    status?: string
    startDate?: string
    endDate?: string
  }

  type ResetPasswordRequest = {
    email: string
    code: string
    newPassword: string
    checkPassword: string
  }

  type resetUserQuotaParams = {
    userId: number
  }

  type SendEmailCodeRequest = {
    email: string
    scene: string
  }

  type stripeSuccessParams = {
    session_id: string
  }

  type User = {
    id?: number
    userAccount?: string
    userEmail?: string
    userPassword?: string
    userName?: string
    userAvatar?: string
    userProfile?: string
    userRole?: string
    editTime?: string
    createTime?: string
    updateTime?: string
    isDelete?: number
    tokenQuota?: number
    usedTokens?: number
    userStatus?: string
    balance?: number
  }

  type UserAddRequest = {
    userName?: string
    userAccount?: string
    userAvatar?: string
    userProfile?: string
    userRole?: string
  }

  type UserAnalysisVO = {
    userId?: number
    userAccount?: string
    userName?: string
    userStatus?: string
    userRole?: string
    tokenQuota?: number
    usedTokens?: number
    remainingQuota?: number
    totalRequests?: number
    successRequests?: number
    totalTokens?: number
    totalCost?: number
    todayCost?: number
  }

  type UserEmailLoginRequest = {
    email: string
    code: string
  }

  type UserEmailRegisterRequest = {
    email: string
    code: string
    userPassword: string
    checkPassword: string
  }

  type UserLoginRequest = {
    userAccount?: string
    userPassword?: string
  }

  type UserQueryRequest = {
    pageNum?: number
    pageSize?: number
    sortField?: string
    sortOrder?: string
    id?: number
    userName?: string
    userAccount?: string
    userProfile?: string
    userRole?: string
  }

  type UserRegisterRequest = {
    userAccount?: string
    userPassword?: string
    checkPassword?: string
  }

  type UserSummaryStatsVO = {
    totalTokens?: number
    tokenQuota?: number
    usedTokens?: number
    remainingQuota?: number
    totalCost?: number
    todayCost?: number
    totalRequests?: number
    successRequests?: number
  }

  type UserUpdateRequest = {
    id?: number
    userName?: string
    userAvatar?: string
    userProfile?: string
    userRole?: string
    tokenQuota?: number
  }

  type UserVO = {
    id?: number
    userAccount?: string
    userName?: string
    userEmail?: string
    userAvatar?: string
    userProfile?: string
    userRole?: string
    createTime?: string
    tokenQuota?: number
    usedTokens?: number
    userStatus?: string
    balance?: number
  }
}
