package com.ryusgua.windows;

import com.ryusgua.app.HexagramEngine;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

public final class WindowsApp {
    private static final Color BG = new Color(14,23,20);
    private static final Color PANEL = new Color(20,31,27);
    private static final Color FG = new Color(241,234,212);
    private static final Color MUTED = new Color(155,166,150);
    private static final Color GOLD = new Color(201,168,91);
    private static final Color GRID = new Color(48,61,53);

    private final JFrame frame = new JFrame("柳之卦 · Ryu's Gua — Windows");
    private final HexPanel hexPanel = new HexPanel();
    private final JTextArea overview = area();
    private final JTextArea reading = area();
    private final JTextArea classics = area();
    private final JComboBox<Integer>[] manual = new JComboBox[6];
    private final DefaultListModel<HistoryStore.Entry> historyModel = new DefaultListModel<>();
    private final JList<HistoryStore.Entry> historyList = new JList<>(historyModel);
    private final JLabel status = new JLabel("READY");
    private final ZhouYiRepository zhouYi = new ZhouYiRepository();
    private final HistoryStore history = new HistoryStore();
    private int[] current;

    public static void main(String[] args) {
        if (args.length > 0 && "--self-test".equals(args[0])) {
            selfTest();
            return;
        }
        SwingUtilities.invokeLater(() -> new WindowsApp().show());
    }

    private static void selfTest() {
        ZhouYiRepository repo = new ZhouYiRepository();
        int[] qian = {7,7,7,7,7,7};
        HexagramEngine.Hexagram h = HexagramEngine.lookup(qian,false);
        if (h.number != 1 || !"乾为天".equals(h.name)) throw new IllegalStateException("lookup failed");
        if (repo.get("乾为天").guaCi.contains("暂无")) throw new IllegalStateException("zhouyi failed");
        int[] kunMoving = {6,6,6,6,6,6};
        if (HexagramEngine.lookup(kunMoving,true).number != 1) throw new IllegalStateException("changed hexagram failed");
        if (!OfflineInterpreter.interpret(kunMoving,repo).contains("用六")) throw new IllegalStateException("offline interpreter failed");
        System.out.println("RyusGua Windows self-test OK");
    }

    private WindowsApp() {
        applyTheme();
        buildUi();
        reloadHistory();
    }

    private void show() {
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setMinimumSize(new Dimension(980,700));
        frame.setSize(1120,780);
        frame.setLocationRelativeTo(null);
        frame.setIconImage(AppIcon.create(256));
        frame.setVisible(true);
    }

    private void buildUi() {
        StripePanel root = new StripePanel(new BorderLayout(16,16));
        root.setBorder(new EmptyBorder(18,20,18,20));
        frame.setContentPane(root);

        JPanel header = transparent(new BorderLayout());
        JLabel title = new JLabel("柳 之 卦");
        title.setFont(font(Font.BOLD,28));
        title.setForeground(FG);
        JLabel sub = new JLabel("RYU'S GUA · WINDOWS DESKTOP");
        sub.setFont(font(Font.PLAIN,11));
        sub.setForeground(MUTED);
        JPanel stack = transparent();
        stack.setLayout(new BoxLayout(stack,BoxLayout.Y_AXIS));
        stack.add(title); stack.add(sub);
        header.add(stack,BorderLayout.WEST);
        status.setForeground(GOLD); status.setFont(font(Font.BOLD,11));
        header.add(status,BorderLayout.EAST);
        root.add(header,BorderLayout.NORTH);

        root.add(controls(),BorderLayout.WEST);

        JPanel center = transparent(new BorderLayout(10,10));
        hexPanel.setPreferredSize(new Dimension(720,300));
        center.add(hexPanel,BorderLayout.NORTH);
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(font(Font.PLAIN,13));
        tabs.addTab("卦象",scroll(overview));
        tabs.addTab("离线解卦",scroll(reading));
        tabs.addTab("经文",scroll(classics));
        tabs.addTab("历史",historyTab());
        center.add(tabs,BorderLayout.CENTER);
        root.add(center,BorderLayout.CENTER);
    }

    private JPanel controls() {
        JPanel p = panel();
        p.setPreferredSize(new Dimension(255,0));
        p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS));
        p.setBorder(new EmptyBorder(16,14,16,14));
        p.add(section("起卦")); p.add(Box.createVerticalStrut(10));
        JButton cast = button("一念既起 · 六爻将成",true);
        cast.addActionListener(e -> randomCast());
        p.add(cast); p.add(Box.createVerticalStrut(8));
        JButton clear = button("清空当前",false);
        clear.addActionListener(e -> clear());
        p.add(clear); p.add(Box.createVerticalStrut(22));

        p.add(section("手动六爻"));
        JLabel hint = new JLabel("上爻在上 · 初爻在下");
        hint.setForeground(MUTED); hint.setFont(font(Font.PLAIN,10));
        p.add(hint); p.add(Box.createVerticalStrut(8));

        Integer[] vals={6,7,8,9};
        for(int i=5;i>=0;i--){
            JPanel row=transparent(new BorderLayout(8,0));
            JLabel l=new JLabel(pos(i)); l.setForeground(FG);
            manual[i]=new JComboBox<>(vals);
            manual[i].setSelectedItem(i%2==0?7:8);
            row.add(l,BorderLayout.WEST); row.add(manual[i],BorderLayout.CENTER);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE,30));
            p.add(row); p.add(Box.createVerticalStrut(4));
        }
        JButton manualBtn=button("按六爻成卦",false);
        manualBtn.addActionListener(e -> manualCast());
        p.add(manualBtn); p.add(Box.createVerticalStrut(22));

        p.add(section("工具"));
        JButton copy=button("复制当前解卦",false);
        copy.addActionListener(e -> copy());
        p.add(copy); p.add(Box.createVerticalStrut(8));
        JButton about=button("关于 Windows 版",false);
        about.addActionListener(e -> JOptionPane.showMessageDialog(frame,
                "柳之卦 · Ryu's Gua\nWindows Desktop v1.0.0\n\n三钱六爻、经文与离线解卦均在本机运行。\nAndroid 与 Windows 分开维护版本号。\n\n作者 · Ryyus",
                "关于",JOptionPane.INFORMATION_MESSAGE));
        p.add(about); p.add(Box.createVerticalGlue());
        JLabel v=new JLabel("Windows v1.0.0"); v.setForeground(MUTED); v.setFont(font(Font.PLAIN,10));
        p.add(v);
        return p;
    }

    private JPanel historyTab() {
        JPanel p=panel(); p.setLayout(new BorderLayout(8,8)); p.setBorder(new EmptyBorder(10,10,10,10));
        historyList.setFont(font(Font.PLAIN,12));
        p.add(new JScrollPane(historyList),BorderLayout.CENTER);
        JPanel actions=transparent(new FlowLayout(FlowLayout.LEFT,8,0));
        JButton load=button("载入",false);
        load.addActionListener(e -> {var x=historyList.getSelectedValue(); if(x!=null) render(x.lines,false);});
        JButton wipe=button("清空历史",false);
        wipe.addActionListener(e -> {
            if(JOptionPane.showConfirmDialog(frame,"确定清空 Windows 本地历史？","清空历史",JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION){
                history.clear(); reloadHistory();
            }
        });
        actions.add(load); actions.add(wipe); p.add(actions,BorderLayout.SOUTH);
        return p;
    }

    private void randomCast() {
        int[] lines=new int[6];
        for(int i=0;i<6;i++) lines[i]=HexagramEngine.castLine();
        render(lines,true);
    }

    private void manualCast() {
        int[] lines=new int[6];
        for(int i=0;i<6;i++) lines[i]=(Integer)manual[i].getSelectedItem();
        render(lines,true);
    }

    private void render(int[] lines, boolean save) {
        current=Arrays.copyOf(lines,6);
        var base=HexagramEngine.lookup(lines,false);
        var changed=HexagramEngine.lookup(lines,true);
        var moving=HexagramEngine.movingLineLabels(lines);
        hexPanel.set(current,base,changed);

        StringBuilder o=new StringBuilder();
        o.append(base.compact()).append("（上").append(base.upper).append("下").append(base.lower).append("）\n");
        if(moving.isEmpty()) o.append("无动爻 · 本卦不变\n");
        else o.append("动爻：").append(String.join("、",moving)).append("\n之卦：").append(changed.compact()).append("\n");
        o.append("\n六爻（自下而上）：\n");
        for(int i=0;i<6;i++) o.append(pos(i)).append("  ").append(lines[i]).append("  ").append(HexagramEngine.lineText(lines[i])).append("\n");
        overview.setText(o.toString());
        reading.setText(OfflineInterpreter.interpret(lines,zhouYi));
        classics.setText(classics(lines,base,changed));
        status.setText(base.name+(moving.isEmpty()?" · STATIC":" → "+changed.name));
        if(save){history.add(lines);reloadHistory();}
    }

    private String classics(int[] lines, HexagramEngine.Hexagram base, HexagramEngine.Hexagram changed) {
        StringBuilder b=new StringBuilder();
        var t=zhouYi.get(base.name);
        b.append("【").append(base.compact()).append("】\n卦辞：").append(t.guaCi).append("\n\n");
        for(int i=0;i<6;i++){
            boolean yang=HexagramEngine.isYang(lines[i]);
            b.append(ZhouYiRepository.label(i,yang)).append("：").append(zhouYi.line(base.name,i,yang));
            if(HexagramEngine.isMoving(lines[i]))b.append("  ← 动");
            b.append("\n");
        }
        boolean moving=false; for(int v:lines)moving|=HexagramEngine.isMoving(v);
        if(moving){
            var c=zhouYi.get(changed.name);
            b.append("\n【之卦 · ").append(changed.compact()).append("】\n卦辞：").append(c.guaCi).append("\n\n");
            for(int i=0;i<6;i++){
                boolean yang=HexagramEngine.changedYang(lines[i]);
                b.append(ZhouYiRepository.label(i,yang)).append("：").append(zhouYi.line(changed.name,i,yang)).append("\n");
            }
        }
        return b.toString();
    }

    private void clear(){current=null;hexPanel.set(null,null,null);overview.setText("");reading.setText("");classics.setText("");status.setText("READY");}
    private void copy(){if(current==null)return;Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(overview.getText()+"\n\n"+reading.getText()),null);status.setText("COPIED");}
    private void reloadHistory(){historyModel.clear();for(var e:history.load())historyModel.addElement(e);}

    private static JTextArea area(){JTextArea a=new JTextArea();a.setEditable(false);a.setLineWrap(true);a.setWrapStyleWord(true);a.setMargin(new Insets(14,16,14,16));a.setFont(font(Font.PLAIN,14));return a;}
    private static JScrollPane scroll(Component c){JScrollPane s=new JScrollPane(c);s.setBorder(BorderFactory.createLineBorder(GRID));return s;}
    private static JPanel panel(){JPanel p=new JPanel();p.setBackground(PANEL);p.setBorder(BorderFactory.createLineBorder(GRID));return p;}
    private static JPanel transparent(){JPanel p=new JPanel();p.setOpaque(false);return p;}
    private static JPanel transparent(LayoutManager l){JPanel p=new JPanel(l);p.setOpaque(false);return p;}
    private static JLabel section(String s){JLabel l=new JLabel(s);l.setForeground(GOLD);l.setFont(font(Font.BOLD,13));l.setAlignmentX(Component.LEFT_ALIGNMENT);return l;}
    private static Font font(int style,int size){return new Font("Microsoft YaHei UI",style,size);}
    private static String pos(int i){return switch(i){case 0->"初爻";case 1->"二爻";case 2->"三爻";case 3->"四爻";case 4->"五爻";case 5->"上爻";default->"";};}

    private static JButton button(String text,boolean primary){
        JButton b=new JButton(text);b.setFont(font(primary?Font.BOLD:Font.PLAIN,primary?13:12));b.setForeground(primary?BG:FG);b.setBackground(primary?GOLD:new Color(29,43,37));b.setFocusPainted(false);
        b.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(primary?GOLD:GRID),new EmptyBorder(9,12,9,12)));b.setAlignmentX(Component.LEFT_ALIGNMENT);b.setMaximumSize(new Dimension(Integer.MAX_VALUE,42));return b;
    }

    private static void applyTheme(){
        UIManager.put("Panel.background",PANEL);UIManager.put("Label.foreground",FG);UIManager.put("Button.background",new Color(29,43,37));UIManager.put("Button.foreground",FG);
        UIManager.put("TextArea.background",new Color(12,19,17));UIManager.put("TextArea.foreground",FG);UIManager.put("TextArea.caretForeground",GOLD);
        UIManager.put("TabbedPane.background",PANEL);UIManager.put("TabbedPane.foreground",FG);UIManager.put("List.background",new Color(12,19,17));UIManager.put("List.foreground",FG);
        UIManager.put("ComboBox.background",new Color(29,43,37));UIManager.put("ComboBox.foreground",FG);
    }

    private static final class StripePanel extends JPanel {
        StripePanel(LayoutManager l){super(l);setBackground(BG);}
        protected void paintComponent(Graphics g){super.paintComponent(g);Graphics2D x=(Graphics2D)g.create();x.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);x.setColor(new Color(233,227,209,18));x.setStroke(new BasicStroke(1.2f));for(int i=-2;i<9;i++)x.drawArc(-80,30+i*92-120,getWidth()+180,200,8,160);x.dispose();}
    }

    private static final class HexPanel extends JPanel {
        private int[] lines; private HexagramEngine.Hexagram base,changed;
        HexPanel(){setOpaque(false);setBorder(BorderFactory.createLineBorder(GRID));}
        void set(int[] l,HexagramEngine.Hexagram b,HexagramEngine.Hexagram c){lines=l==null?null:Arrays.copyOf(l,6);base=b;changed=c;repaint();}
        protected void paintComponent(Graphics g){super.paintComponent(g);Graphics2D x=(Graphics2D)g.create();x.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);x.setColor(new Color(12,19,17,235));x.fillRect(0,0,getWidth(),getHeight());
            if(lines==null){x.setFont(font(Font.BOLD,26));x.setColor(FG);center(x,"一念既起 · 六爻将成",getWidth()/2,getHeight()/2);x.setFont(font(Font.PLAIN,11));x.setColor(MUTED);center(x,"THREE COINS · SIX LINES · OFFLINE",getWidth()/2,getHeight()/2+28);x.dispose();return;}
            int mid=getWidth()/2;drawHex(x,false,mid/2,base);boolean m=false;for(int v:lines)m|=HexagramEngine.isMoving(v);if(m)drawHex(x,true,mid+mid/2,changed);else{x.setColor(MUTED);center(x,"无动爻 · NO CHANGE",mid+mid/2,getHeight()/2);}x.setColor(GRID);x.drawLine(mid,28,mid,getHeight()-28);x.dispose();}
        private void drawHex(Graphics2D x,boolean changedMode,int cx,HexagramEngine.Hexagram h){x.setFont(font(Font.BOLD,17));x.setColor(FG);center(x,(changedMode?"之卦 · ":"本卦 · ")+h.name,cx,42);x.setFont(font(Font.PLAIN,10));x.setColor(MUTED);center(x,"第"+h.number+"卦 · 上"+h.upper+"下"+h.lower,cx,62);
            int w=128,start=92,gap=28;for(int d=5;d>=0;d--){int row=5-d,y=start+row*gap;boolean yang=changedMode?HexagramEngine.changedYang(lines[d]):HexagramEngine.isYang(lines[d]);boolean moving=!changedMode&&HexagramEngine.isMoving(lines[d]);x.setStroke(new BasicStroke(7f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));x.setColor(moving?GOLD:FG);if(yang)x.drawLine(cx-w/2,y,cx+w/2,y);else{x.drawLine(cx-w/2,y,cx-12,y);x.drawLine(cx+12,y,cx+w/2,y);}if(moving){x.setColor(GOLD);x.drawString(lines[d]==6?"×":"○",cx+w/2+18,y+4);}}}
        private static void center(Graphics2D g,String s,int x,int y){FontMetrics f=g.getFontMetrics();g.drawString(s,x-f.stringWidth(s)/2,y);}
    }

    private static final class AppIcon {
        static Image create(int size){java.awt.image.BufferedImage i=new java.awt.image.BufferedImage(size,size,java.awt.image.BufferedImage.TYPE_INT_ARGB);Graphics2D g=i.createGraphics();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);g.setColor(BG);g.fillRect(0,0,size,size);g.setColor(FG);g.setStroke(new BasicStroke(Math.max(4,size/32f),BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));int l=size*24/100,r=size*59/100;for(int n=0;n<6;n++){int y=size*(31+n*7)/100;if(n==1||n==4){g.drawLine(l,y,size*38/100,y);g.drawLine(size*45/100,y,r,y);}else g.drawLine(l,y,r,y);}g.setColor(new Color(168,181,140));g.setStroke(new BasicStroke(Math.max(3,size/50f)));g.drawArc(size*48/100,size*17/100,size*38/100,size*62/100,235,190);g.dispose();return i;}
    }

    static final class ZhouYiRepository {
        static final class Entry { final String guaCi; final LinkedHashMap<String,String> yao; Entry(String g,LinkedHashMap<String,String> y){guaCi=g;yao=y;} }
        private final Map<String,Entry> entries=new LinkedHashMap<>();
        ZhouYiRepository(){try(InputStream in=WindowsApp.class.getResourceAsStream("/zhouyi.json")){if(in==null)throw new IOException("zhouyi.json missing");Map<?,?> root=(Map<?,?>)new Json(new String(in.readAllBytes(),StandardCharsets.UTF_8)).parse();for(var e:root.entrySet()){String name=String.valueOf(e.getKey());Map<?,?> o=(Map<?,?>)e.getValue();Object gc=o.get("gua_ci");LinkedHashMap<String,String> y=new LinkedHashMap<>();if(o.get("yao") instanceof Map<?,?> ym)for(var x:ym.entrySet())y.put(String.valueOf(x.getKey()),String.valueOf(x.getValue()));entries.put(name,new Entry(gc==null?"":String.valueOf(gc),y));}fix();}catch(Exception e){throw new IllegalStateException("无法加载周易数据",e);}}
        Entry get(String n){return entries.getOrDefault(n,new Entry("暂无经文。",new LinkedHashMap<>()));}
        String line(String n,int i,boolean yang){Entry e=get(n);String v=e.yao.get(label(i,yang));if(v!=null)return v;List<String> a=new ArrayList<>(e.yao.values());return i<a.size()?a.get(i):"暂无爻辞。";}
        static String label(int i,boolean yang){String p=yang?"九":"六";if(i==0)return "初"+p;if(i==5)return "上"+p;return p+(i+1);}
        private void fix(){Entry shi=entries.get("地水师");if(shi!=null){LinkedHashMap<String,String> f=new LinkedHashMap<>();f.put("初六","师出以律，否臧凶。");f.put("九二","在师中，吉，无咎，王三锡命。");f.put("六三","师或舆尸，凶。");f.put("六四","师左次，无咎。");f.put("六五","田有禽，利执言，无咎。长子帅师，弟子舆尸，贞凶。");f.put("上六","大君有命，开国承家，小人勿用。");entries.put("地水师",new Entry("贞，丈人吉，无咎。",f));}Entry k=entries.get("泽水困");if(k!=null)entries.put("泽水困",new Entry("亨，贞，大人吉，无咎，有言不信。",k.yao));}
    }

    static final class OfflineInterpreter {
        static String interpret(int[] lines,ZhouYiRepository repo){
            var base=HexagramEngine.lookup(lines,false);var changed=HexagramEngine.lookup(lines,true);var bt=repo.get(base.name);var ct=repo.get(changed.name);List<Integer> moving=new ArrayList<>();for(int i=0;i<6;i++)if(HexagramEngine.isMoving(lines[i]))moving.add(i);
            StringBuilder b=new StringBuilder("【卦意】\n").append(base.compact()).append("（上").append(base.upper).append("下").append(base.lower).append("）\n卦辞：").append(bt.guaCi).append("\n\n【取用】\n");
            int c=moving.size();
            if(c==0)b.append("无动爻，以本卦卦辞为主。\n");
            else if(c==1){int i=moving.get(0);boolean y=HexagramEngine.isYang(lines[i]);b.append("一爻动，以该爻为主：\n★ ").append(ZhouYiRepository.label(i,y)).append("：").append(repo.line(base.name,i,y)).append("\n");}
            else if(c==2){b.append("两爻动，同时参考两爻，以上位动爻为后续重点：\n");for(int i:moving){boolean y=HexagramEngine.isYang(lines[i]);b.append("· ").append(ZhouYiRepository.label(i,y)).append("：").append(repo.line(base.name,i,y)).append("\n");}}
            else if(c==3)b.append("三爻动，本卦与之卦并看。\n本卦：").append(bt.guaCi).append("\n之卦：").append(ct.guaCi).append("\n");
            else if(c==6&&"乾为天".equals(base.name)&&bt.yao.containsKey("用九"))b.append("六爻皆动，取用九：").append(bt.yao.get("用九")).append("\n");
            else if(c==6&&"坤为地".equals(base.name)&&bt.yao.containsKey("用六"))b.append("六爻皆动，取用六：").append(bt.yao.get("用六")).append("\n");
            else b.append("变化占多数，判断重心转向之卦：").append(ct.guaCi).append("\n");
            if(c>0)b.append("\n【之卦】\n").append(changed.compact()).append("（上").append(changed.upper).append("下").append(changed.lower).append("）\n之卦卦辞：").append(ct.guaCi).append("\n");
            b.append("\n【建议】\n").append(advice(c)).append("\n\nWindows 离线解卦由本机规则生成，无需网络；内容仅供传统文化阅读与娱乐参考。");
            return b.toString();
        }
        private static String advice(int c){return switch(c){case 0->"事势相对稳定，先守住本卦主轴。";case 1->"集中处理唯一动爻所指向的环节。";case 2->"两个变化点并存，先处理较早、较低层因素。";case 3->"处在转折区间，把本卦看作现状、之卦看作趋势。";case 4->"变化已占多数，判断重心逐渐转向之卦。";case 5->"局势接近整体转换，注意变化中的唯一稳定点。";default->"整体转换最强，以变化后的整体方向为主。";};}
    }

    static final class HistoryStore {
        static final class Entry {final LocalDateTime time;final int[] lines;Entry(LocalDateTime t,int[] l){time=t;lines=Arrays.copyOf(l,6);}public String toString(){return time.format(DateTimeFormatter.ofPattern("MM-dd HH:mm:ss"))+"  "+HexagramEngine.lookup(lines,false).name+"  "+Arrays.toString(lines);}}
        private final Path file;
        HistoryStore(){String a=System.getenv("APPDATA");Path d=(a==null||a.isBlank())?Path.of(System.getProperty("user.home"),".ryusgua"):Path.of(a,"RyusGua");file=d.resolve("history.tsv");}
        void add(int[] lines){try{Files.createDirectories(file.getParent());Files.writeString(file,LocalDateTime.now()+"\t"+join(lines)+"\n",StandardCharsets.UTF_8,StandardOpenOption.CREATE,StandardOpenOption.APPEND);trim();}catch(Exception ignored){}}
        List<Entry> load(){List<Entry> out=new ArrayList<>();if(!Files.exists(file))return out;try{List<String> all=Files.readAllLines(file,StandardCharsets.UTF_8);for(int i=all.size()-1;i>=0&&out.size()<30;i--){String[] p=all.get(i).split("\t");if(p.length==2){int[] l=parse(p[1]);if(l!=null)out.add(new Entry(LocalDateTime.parse(p[0]),l));}}}catch(Exception ignored){}return out;}
        void clear(){try{Files.deleteIfExists(file);}catch(Exception ignored){}}
        private void trim()throws IOException{List<String> a=Files.readAllLines(file,StandardCharsets.UTF_8);if(a.size()>30)Files.write(file,a.subList(a.size()-30,a.size()),StandardCharsets.UTF_8);}
        private static String join(int[] a){StringJoiner j=new StringJoiner(",");for(int v:a)j.add(String.valueOf(v));return j.toString();}
        private static int[] parse(String s){String[] p=s.split(",");if(p.length!=6)return null;int[] a=new int[6];try{for(int i=0;i<6;i++)a[i]=Integer.parseInt(p[i]);return a;}catch(Exception e){return null;}}
    }

    static final class Json {
        private final String s;private int p;Json(String s){this.s=s;}Object parse(){skip();return val();}
        private Object val(){skip();char c=s.charAt(p);if(c=='{')return obj();if(c=='"')return str();if(s.startsWith("null",p)){p+=4;return null;}throw new IllegalArgumentException("json at "+p);}
        private Map<String,Object> obj(){LinkedHashMap<String,Object> m=new LinkedHashMap<>();eat('{');skip();if(peek('}')){p++;return m;}while(true){String k=str();eat(':');m.put(k,val());skip();if(peek('}')){p++;return m;}eat(',');}}
        private String str(){eat('"');StringBuilder b=new StringBuilder();while(true){char c=s.charAt(p++);if(c=='"')return b.toString();if(c=='\\'){char e=s.charAt(p++);switch(e){case '"','\\','/'->b.append(e);case 'b'->b.append('\b');case 'f'->b.append('\f');case 'n'->b.append('\n');case 'r'->b.append('\r');case 't'->b.append('\t');case 'u'->{b.append((char)Integer.parseInt(s.substring(p,p+4),16));p+=4;}default->throw new IllegalArgumentException("escape");}}else b.append(c);}}
        private void skip(){while(p<s.length()&&Character.isWhitespace(s.charAt(p)))p++;}private boolean peek(char c){skip();return p<s.length()&&s.charAt(p)==c;}private void eat(char c){skip();if(s.charAt(p)!=c)throw new IllegalArgumentException("expected "+c);p++;}
    }
}
