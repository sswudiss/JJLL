import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'
import { serve } from 'https://deno.land/std@0.168.0/http/server.ts'

console.log('Function "send-push-notification" up and running!')

serve(async (req) => {
  try {
    // 1. 從觸發器獲取新消息的數據
    // 當由數據庫觸發器調用時，請求體會包含 { type: "INSERT", record: {...}, ... }
    const { record: newMessage, type } = await req.json()

    // 確保這是一個 INSERT 事件
    if (type !== 'INSERT') {
      return new Response('Not an insert event', { status: 200 })
    }

    console.log('New message received:', newMessage)

    // 2. 創建一個具有管理員權限的 Supabase 客戶端
    // 這些環境變量需要在部署時或本地運行時設置
    const supabaseAdmin = createClient(
      Deno.env.get('SUPABASE_URL')!,
      Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!
    )

    // 3. 根據 chat_id 找到所有參與者，並排除發送者
    const { data: participants, error: pError } = await supabaseAdmin
      .from('chat_participants')
      .select('user_id')
      .eq('chat_id', newMessage.chat_id)
      .neq('user_id', newMessage.sender_id)

    if (pError) {
      throw new Error(`Error fetching participants: ${pError.message}`)
    }

    if (!participants || participants.length === 0) {
      console.log('No other participants in this chat. No notification sent.')
      return new Response('No recipients', { status: 200 })
    }

    console.log('Found participants to notify:', participants)
    const receiverIds = participants.map(p => p.user_id)

    // 4. 獲取所有接收者的 FCM tokens
    const { data: profiles, error: prError } = await supabaseAdmin
      .from('profiles')
      .select('fcm_token')
      .in('user_id', receiverIds)
      .not('fcm_token', 'is', null) // 只選擇 fcm_token 不為空的

    if (prError) {
      throw new Error(`Error fetching profiles/tokens: ${prError.message}`)
    }

    if (!profiles || profiles.length === 0) {
      console.log('No valid FCM tokens found for recipients.')
      return new Response('No tokens found', { status: 200 })
    }
    
    const tokens = profiles.map(p => p.fcm_token)
    console.log(`Found ${tokens.length} tokens to send notification to:`, tokens)

    // --- 在下一階段，我們將在這裡添加發送 FCM 通知的代碼 ---
    console.log('TODO: Implement FCM push notification sending here.')

    // 5. 返回一個成功的響應
    return new Response(JSON.stringify({ success: true, tokensFound: tokens }), {
      headers: { 'Content-Type': 'application/json' },
    })
  } catch (error) {
    console.error('Error in function:', error.message)
    return new Response(JSON.stringify({ error: error.message }), {
      status: 500,
      headers: { 'Content-Type': 'application/json' },
    })
  }
})