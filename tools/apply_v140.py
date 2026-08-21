from pathlib import Path

p=Path('app/src/main/java/com/ryusgua/app/MainActivity.java')
s=p.read_text(encoding='utf-8')

def rep(old,new,count=None):
    global s
    n=s.count(old)
    if n==0:
        raise SystemExit('missing patch anchor: '+old[:120].replace('\n','\\n'))
    if count is not None and n!=count:
        raise SystemExit(f'anchor count {n} != {count}: '+old[:80].replace('\n','\\n'))
    s=s.replace(old,new)

rep('private enum State { BOOT, IDLE, CASTING, RESULT, DETAIL, HISTORY, OFFLINE, AI }',
    'private enum State { BOOT, IDLE, CASTING, RESULT, DETAIL, BOARD, HISTORY, OFFLINE, AI }',1)

rep('''        private final RectF reasoningToggleButton = new RectF();
        private final String[] currentCoins = {"·", "·", "·"};
        private final ZhouYiRepository zhouYi;
        private final ArrayList<HistoryHit> historyHits = new ArrayList<>();''',
'''        private final RectF reasoningToggleButton = new RectF();
        private final RectF copyButton = new RectF();
        private final String[] currentCoins = {"·", "·", "·"};
        private final ZhouYiRepository zhouYi;
        private final LiuYaoKnowledgeRepository liuYaoKnowledge;
        private final ArrayList<HistoryHit> historyHits = new ArrayList<>();''',1)

rep('''        private boolean loadedFromHistory = false;
        private String currentHistoryId = "";
        private boolean aiLoading = false;''',
'''        private boolean loadedFromHistory = false;
        private String currentHistoryId = "";
        private long currentCastTimeMillis = 0L;
        private String yongshenQin = "";
        private boolean aiLoading = false;''',1)

rep('''            zhouYi = new ZhouYiRepository(context);
            paint.setTypeface(mono);''',
'''            zhouYi = new ZhouYiRepository(context);
            liuYaoKnowledge = new LiuYaoKnowledgeRepository(context);
            paint.setTypeface(mono);''',1)

rep('''            if (state == State.DETAIL) { state = State.RESULT; scrollOffset = 0; postInvalidateOnAnimation(); return true; }
            if (state == State.HISTORY) { state = State.IDLE; scrollOffset = 0; postInvalidateOnAnimation(); return true; }''',
'''            if (state == State.DETAIL) { state = State.RESULT; scrollOffset = 0; postInvalidateOnAnimation(); return true; }
            if (state == State.BOARD) { state = State.RESULT; scrollOffset = 0; postInvalidateOnAnimation(); return true; }
            if (state == State.HISTORY) { state = State.IDLE; scrollOffset = 0; postInvalidateOnAnimation(); return true; }''',1)

rep('''                case RESULT: drawResult(c, contentW, contentH); break;
                case DETAIL: drawDetail(c, contentW, contentH); break;
                case HISTORY: drawHistory(c, contentW, contentH); break;''',
'''                case RESULT: drawResult(c, contentW, contentH); break;
                case DETAIL: drawDetail(c, contentW, contentH); break;
                case BOARD: drawBoard(c, contentW, contentH); break;
                case HISTORY: drawHistory(c, contentW, contentH); break;''',1)

old='''            text(c, "KEY MAP", dp(22), dp(421), 7.5f, MUTED, Paint.Align.LEFT, true);
            float y1 = h - dp(154), y2 = h - dp(99), y3 = h - dp(44);
            auxLeftButton.set(dp(20), y1, w/3f-dp(4), y2-dp(7));
            rightButton.set(w/3f+dp(4), y1, w*2f/3f-dp(4), y2-dp(7));
            auxRightButton.set(w*2f/3f+dp(4), y1, w-dp(20), y2-dp(7));
            leftButton.set(dp(20), y2, w/2f-dp(5), y3-dp(7));
            settingsButton.set(w/2f+dp(5), y2, w-dp(20), y3-dp(7));
            primaryButton.set(dp(20), y3, w-dp(20), h-dp(8));
            button(c, auxLeftButton, "经文", GOLD, false, 11);
            button(c, rightButton, "解卦", GOLD, true, 11);
            button(c, auxRightButton, "历史", MUTED, false, 11);
            button(c, leftButton, "复制", FG, false, 10);
            button(c, settingsButton, "设置", MUTED, false, 10);
            button(c, primaryButton, "再起一卦 / RECAST", MUTED, false, 9);'''
new='''            text(c, "KEY MAP", dp(22), dp(421), 7.5f, MUTED, Paint.Align.LEFT, true);
            float y1 = h - dp(154), y2 = h - dp(99), y3 = h - dp(44);
            auxLeftButton.set(dp(20), y1, w/3f-dp(4), y2-dp(7));
            rightButton.set(w/3f+dp(4), y1, w*2f/3f-dp(4), y2-dp(7));
            auxRightButton.set(w*2f/3f+dp(4), y1, w-dp(20), y2-dp(7));
            leftButton.set(dp(20), y2, w/3f-dp(4), y3-dp(7));
            copyButton.set(w/3f+dp(4), y2, w*2f/3f-dp(4), y3-dp(7));
            settingsButton.set(w*2f/3f+dp(4), y2, w-dp(20), y3-dp(7));
            primaryButton.set(dp(20), y3, w-dp(20), h-dp(8));
            button(c, auxLeftButton, "经文", GOLD, false, 11);
            button(c, rightButton, "排盘", GOLD, true, 11);
            button(c, auxRightButton, "历史", MUTED, false, 11);
            button(c, leftButton, "解卦", GOLD, false, 10);
            button(c, copyButton, "复制", FG, false, 10);
            button(c, settingsButton, "设置", MUTED, false, 10);
            button(c, primaryButton, "再起一卦 / RECAST", MUTED, false, 9);'''
rep(old,new,1)

anchor='''        private void drawDetail(Canvas c, float w, float h) {'''
board_methods=r'''        private LiuYaoBoard.Board currentLiuYaoBoard() {
            long when = currentCastTimeMillis > 0L ? currentCastTimeMillis : System.currentTimeMillis();
            return LiuYaoBoard.cast(lines, when, yongshenQin);
        }

        private void drawBoard(Canvas c, float w, float h) {
            backButton.set(w - dp(78), dp(86), w - dp(20), dp(116));
            button(c, backButton, "返回", MUTED, false, 9);
            text(c, "六爻排盘", dp(20), dp(108), 15, GOLD, Paint.Align.LEFT, true);
            text(c, "NAJIA / LOCAL BOARD", dp(20), dp(124), 7.5f, MUTED, Paint.Align.LEFT, true);
            LiuYaoBoard.Board board = currentLiuYaoBoard();
            c.save();
            c.clipRect(0, dp(136), w, h - dp(82));
            float y = dp(158) - scrollOffset;
            y = wrapped(c, board.toPlainText(), dp(20), y, w - dp(40), 9.6f, FG, dp(18), false) + dp(18);
            y = wrapped(c, liuYaoKnowledge.relevantDigest(board), dp(20), y, w - dp(40), 9.2f, MUTED, dp(17), false) + dp(18);
            maxScroll = Math.max(0, y + scrollOffset - (h - dp(82)) + dp(20));
            c.restore();
            auxLeftButton.set(dp(20), h-dp(69), w/3f-dp(4), h-dp(15));
            rightButton.set(w/3f+dp(4), h-dp(69), w*2f/3f-dp(4), h-dp(15));
            auxRightButton.set(w*2f/3f+dp(4), h-dp(69), w-dp(20), h-dp(15));
            button(c, auxLeftButton, yongshenQin.isEmpty() ? "取用神" : yongshenQin, GOLD, false, 9.5f);
            button(c, rightButton, "复制排盘", FG, false, 9.5f);
            button(c, auxRightButton, "术理索引", GOLD, true, 9.5f);
        }

        private void showYongshenDialog() {
            final String[] items = {"未指定", "父母", "兄弟", "子孙", "妻财", "官鬼"};
            int checked = 0;
            for (int i=1;i<items.length;i++) if (items[i].equals(yongshenQin)) checked=i;
            AlertDialog dialog = new AlertDialog.Builder(getContext())
                    .setTitle("用神取用")
                    .setSingleChoiceItems(items, checked, (d, which) -> {
                        yongshenQin = which == 0 ? "" : items[which];
                        scrollOffset = 0;
                        d.dismiss();
                        postInvalidateOnAnimation();
                    })
                    .setNegativeButton("取消", null).create();
            dialog.setOnShowListener(d -> styleSettingsDialog(dialog));
            dialog.show();
        }

        private void copyBoard() {
            ClipboardManager cb = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
            cb.setPrimaryClip(ClipData.newPlainText("柳之卦六爻排盘", currentLiuYaoBoard().toPlainText()));
            haptic(HapticFeedbackConstants.CLOCK_TICK);
            Toast.makeText(getContext(), "六爻排盘已复制", Toast.LENGTH_SHORT).show();
        }

        private void showKnowledgeIndexDialog() {
            List<LiuYaoKnowledgeRepository.Topic> topics = liuYaoKnowledge.all();
            String[] items = new String[topics.size()];
            for (int i=0;i<topics.size();i++) items[i] = topics.get(i).displayTitle();
            AlertDialog dialog = new AlertDialog.Builder(getContext())
                    .setTitle("术理索引")
                    .setItems(items, (d, which) -> showKnowledgeTopic(which))
                    .setNegativeButton("关闭", null).create();
            dialog.setOnShowListener(d -> styleSettingsDialog(dialog));
            dialog.show();
        }

        private void showKnowledgeTopic(int index) {
            LiuYaoKnowledgeRepository.Topic topic = liuYaoKnowledge.at(index);
            if (topic == null) return;
            TextView text = new TextView(getContext());
            text.setText(topic.summary + "\n\n" + topic.body + "\n\n来源：Johnson-Jia / liuyao-divination（MIT），柳之卦本地整理。不同流派可能存在不同取法。");
            text.setTextColor(FG); text.setTextSize(12); text.setLineSpacing(0, 1.28f);
            int pad=(int)dp(16); text.setPadding(pad,pad,pad,pad); text.setBackgroundColor(BG);
            ScrollView scroll=new ScrollView(getContext()); scroll.addView(text);
            AlertDialog dialog = new AlertDialog.Builder(getContext()).setTitle(topic.displayTitle()).setView(scroll)
                    .setPositiveButton("知悉", null).create();
            dialog.setOnShowListener(d -> styleSettingsDialog(dialog)); dialog.show();
        }

'''
rep(anchor,board_methods+anchor,1)

rep('''            if ((state == State.DETAIL || state == State.HISTORY || state == State.OFFLINE || state == State.AI)) {''',
'''            if ((state == State.DETAIL || state == State.BOARD || state == State.HISTORY || state == State.OFFLINE || state == State.AI)) {''',1)

rep('''                        if (state == State.DETAIL || state == State.OFFLINE) state = State.RESULT;
                        else if (state == State.AI) state = State.OFFLINE;
                        else state = State.IDLE;''',
'''                        if (state == State.DETAIL || state == State.BOARD || state == State.OFFLINE) state = State.RESULT;
                        else if (state == State.AI) state = State.OFFLINE;
                        else state = State.IDLE;''',1)

insert='''                    if (!dragging && state == State.BOARD && auxLeftButton.contains(x, y)) { showYongshenDialog(); return true; }
                    if (!dragging && state == State.BOARD && rightButton.contains(x, y)) { copyBoard(); return true; }
                    if (!dragging && state == State.BOARD && auxRightButton.contains(x, y)) { showKnowledgeIndexDialog(); return true; }
'''
needle='''                    if (!dragging && state == State.OFFLINE && auxLeftButton.contains(x, y)) {'''
rep(needle,insert+needle,1)

old_result='''            if (state == State.RESULT) {
                if (auxLeftButton.contains(x, y)) { pulse(10, 60); state = State.DETAIL; scrollOffset = 0; postInvalidateOnAnimation(); return true; }
                if (auxRightButton.contains(x, y)) { pulse(10, 60); state = State.HISTORY; scrollOffset = 0; postInvalidateOnAnimation(); return true; }
                if (leftButton.contains(x, y)) { copyResult(); return true; }
                if (rightButton.contains(x, y)) { state = State.OFFLINE; scrollOffset = 0; haptic(HapticFeedbackConstants.CONFIRM); postInvalidateOnAnimation(); return true; }
                if (settingsButton.contains(x, y)) { showSettingsDialog(false); return true; }
                if (primaryButton.contains(x, y)) { pulse(15, 75); state = State.IDLE; loadedFromHistory = false; postInvalidateOnAnimation(); return true; }
            }'''
new_result='''            if (state == State.RESULT) {
                if (auxLeftButton.contains(x, y)) { pulse(10, 60); state = State.DETAIL; scrollOffset = 0; postInvalidateOnAnimation(); return true; }
                if (rightButton.contains(x, y)) { pulse(10, 60); state = State.BOARD; scrollOffset = 0; haptic(HapticFeedbackConstants.CONFIRM); postInvalidateOnAnimation(); return true; }
                if (auxRightButton.contains(x, y)) { pulse(10, 60); state = State.HISTORY; scrollOffset = 0; postInvalidateOnAnimation(); return true; }
                if (leftButton.contains(x, y)) { state = State.OFFLINE; scrollOffset = 0; haptic(HapticFeedbackConstants.CONFIRM); postInvalidateOnAnimation(); return true; }
                if (copyButton.contains(x, y)) { copyResult(); return true; }
                if (settingsButton.contains(x, y)) { showSettingsDialog(false); return true; }
                if (primaryButton.contains(x, y)) { pulse(15, 75); state = State.IDLE; loadedFromHistory = false; postInvalidateOnAnimation(); return true; }
            }'''
rep(old_result,new_result,1)

# Reset date/use selection for fresh casting in both ordinary and formal starters.
rep('''            currentHistoryId = "";
            aiText = ""; aiReasoning = ""; aiError = ""; aiModel = "";''',
'''            currentHistoryId = "";
            currentCastTimeMillis = 0L;
            yongshenQin = "";
            aiText = ""; aiReasoning = ""; aiError = ""; aiModel = "";''',2)

rep('''            currentHistoryId = entry.id;
            aiText = entry.aiText;''',
'''            currentHistoryId = entry.id;
            currentCastTimeMillis = entry.timeMillis;
            yongshenQin = "";
            aiText = entry.aiText;''',1)

rep('''            currentHistoryId = savedEntry.id;
            recordCompletedCast(wasFormal);''',
'''            currentHistoryId = savedEntry.id;
            currentCastTimeMillis = savedEntry.timeMillis;
            recordCompletedCast(wasFormal);''',1)

rep('''        private String offlineReadingText() {
            return OfflineInterpreter.interpret(lines, zhouYi);
        }''',
'''        private String offlineReadingText() {
            LiuYaoBoard.Board board = currentLiuYaoBoard();
            return OfflineInterpreter.interpret(lines, zhouYi)
                    + "\n\n【六爻盘面】\n" + board.digest();
        }''',1)

old_ai='''        private String aiPrompt() {
            return resultText() + "\n\n【本机离线解卦】\n" + offlineReadingText()
                    + "\n\n请以上述卦象事实与本机离线取用为基础生成最终解读，不要重新计算卦象，也不要复述全部原始数据。"
                    + "按“卦意 / 动爻（如有） / 变卦 / 建议”四部分输出；全文控制在300至450个中文字符，最多不超过500个中文字符。"
                    + "最终回答中不要包含思考过程、分析草稿、reasoning、thinking 或 <think> 标签。";
        }'''
new_ai='''        private String aiPrompt() {
            LiuYaoBoard.Board board = currentLiuYaoBoard();
            return resultText()
                    + "\n\n" + board.toPlainText()
                    + "\n\n" + liuYaoKnowledge.relevantDigest(board)
                    + "\n\n【本机离线解卦】\n" + offlineReadingText()
                    + "\n\n请以上述卦象、六爻排盘事实和本机术理索引为基础生成最终解读，不要重新计算排盘，也不要复述全部原始数据。"
                    + "如未指定用神，不要擅自假定占问类别；按“卦意 / 六爻盘面 / 动变 / 建议”四部分输出。"
                    + "全文控制在350至550个中文字符，最多不超过650个中文字符。最终回答不要包含思考过程、分析草稿、reasoning、thinking 或 <think> 标签。";
        }'''
rep(old_ai,new_ai,1)

settings_anchor='''            LinearLayout update = settingsCard(ctx, "版本更新", "国内优先多源检查");'''
settings_new='''            LinearLayout theory = settingsCard(ctx, "术理索引", "十一专题完全离线");
            root.addView(theory);
            theory.setOnClickListener(v -> showKnowledgeIndexDialog());

            LinearLayout update = settingsCard(ctx, "版本更新", "国内优先多源检查");'''
rep(settings_anchor,settings_new,1)

p.write_text(s,encoding='utf-8')
print('patched MainActivity.java', len(s))
