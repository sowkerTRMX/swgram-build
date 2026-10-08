package org.nqe.sw;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.util.Base64;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import java.security.SecureRandom;
import java.util.ArrayList;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class SwSettingsActivity extends Activity {
    LinearLayout root;
    SharedPreferences sp;
    SecretKey key;
    boolean mainShown = true;
    int tap = 0;
    static final int AC = Color.rgb(56, 160, 255);

    protected void onCreate(Bundle b) {
        super.onCreate(b);
        sp = getSharedPreferences("swgram", 0);
        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(Color.rgb(6, 10, 22));
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(28), dp(16), dp(28));
        sv.addView(root);
        setContentView(sv);
        main();
    }

    public void onBackPressed() { if (!mainShown) main(); else super.onBackPressed(); }

    int dp(int v) { return (int) (v * getResources().getDisplayMetrics().density); }
    void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_SHORT).show(); }
    boolean on(String k) { return sp.getBoolean(k, k.equals("jelly")); }
    boolean isPrime() { return sp.getLong("prime", 0) > System.currentTimeMillis(); }

    TextView t(String s, int size, boolean bold, int col) {
        TextView x = new TextView(this);
        x.setText(s); x.setTextSize(size); x.setTextColor(col);
        if (bold) x.setTypeface(Typeface.DEFAULT_BOLD);
        x.setPadding(0, dp(6), 0, dp(6));
        root.addView(x);
        return x;
    }

    Button bt(String s, View.OnClickListener c) {
        Button x = new Button(this);
        x.setText(s); x.setAllCaps(false); x.setTextColor(Color.WHITE); x.setOnClickListener(c);
        GradientDrawable g = new GradientDrawable();
        g.setColor(Color.rgb(16, 36, 76)); g.setCornerRadius(dp(18));
        x.setBackground(g);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.topMargin = dp(8);
        root.addView(x, p);
        return x;
    }

    void sw(String s, final String k) {
        Switch x = new Switch(this);
        x.setText(s); x.setTextColor(Color.WHITE); x.setChecked(on(k));
        x.setPadding(0, dp(8), 0, dp(8));
        x.setOnCheckedChangeListener((v, c) -> sp.edit().putBoolean(k, c).apply());
        root.addView(x);
    }

    void begin(String title) { mainShown = false; root.removeAllViews(); t(title, 22, true, AC); }

    void main() {
        mainShown = true;
        root.removeAllViews();
        final TextView h = t("swgram", 30, true, AC);
        h.setOnClickListener(v -> { String[] n = {"swgram", "kilogram", "miligram", "maxagram", "pashagram"}; tap++; h.setText(n[tap % n.length]); });
        t("Приватность", 16, true, Color.WHITE);
        sw("Ghost: не показывать прочтение", "ghost");
        sw("Тихие истории: просмотр без следа", "ghoststory");
        sw("Не показывать набор текста", "ghosttype");
        sw("Скрывать онлайн", "offline");
        sw("Запретить скриншоты (после перезапуска)", "noshot");
        sw("Желейные кнопки", "jelly");
        bt("Быстрые чаты (локальный закреп)", v -> list("pins", 250, false));
        bt("sKey: защищённый список чатов", v -> skey());
        bt("Иконка и название", v -> icons());
        bt("VPrime", v -> primeScreen());
        bt("Плагины", v -> plugins());
        bt("Функции sw", v -> funcs());
    }

    String encS(String s) throws Exception {
        byte[] iv = new byte[12];
        new SecureRandom().nextBytes(iv);
        Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
        c.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, iv));
        byte[] ct = c.doFinal(s.getBytes("UTF-8"));
        byte[] o = new byte[12 + ct.length];
        System.arraycopy(iv, 0, o, 0, 12);
        System.arraycopy(ct, 0, o, 12, ct.length);
        return Base64.encodeToString(o, Base64.NO_WRAP);
    }

    String decS(String s) throws Exception {
        byte[] o = Base64.decode(s, Base64.NO_WRAP);
        Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
        c.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, o, 0, 12));
        return new String(c.doFinal(o, 12, o.length - 12), "UTF-8");
    }

    SecretKey derive(String pw, byte[] salt) throws Exception {
        byte[] k = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1").generateSecret(new PBEKeySpec(pw.toCharArray(), salt, 100000, 256)).getEncoded();
        return new SecretKeySpec(k, "AES");
    }

    ArrayList<String> load(String k, boolean crypt) {
        ArrayList<String> L = new ArrayList<String>();
        String s = sp.getString("l_" + k, "");
        try { if (crypt && s.length() > 0) s = decS(s); } catch (Exception e) { s = ""; }
        for (String x : s.split("\n")) if (x.length() > 0) L.add(x);
        return L;
    }

    void store(String k, ArrayList<String> L, boolean crypt) {
        StringBuilder b = new StringBuilder();
        for (String x : L) b.append(x).append("\n");
        String s = b.toString();
        try { if (crypt) s = encS(s); } catch (Exception e) { return; }
        sp.edit().putString("l_" + k, s).apply();
    }

    void open(String u) {
        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("tg://resolve?domain=" + u));
        i.setPackage(getPackageName());
        try { startActivity(i); } catch (Exception e) { toast("Не удалось открыть чат"); }
    }

    void list(final String k, final int lim, final boolean crypt) {
        begin(k.equals("pins") ? "Быстрые чаты" : "sKey");
        t(k.equals("pins") ? "Чат закрепляется локально, по нажатию открывается сам чат." : "Список защищён паролем и зашифрован.", 13, false, Color.GRAY);
        final ArrayList<String> L = load(k, crypt);
        final int max = isPrime() ? Integer.MAX_VALUE : lim;
        for (final String s : L) {
            final String[] p = s.split("\\|", 2);
            Button x = bt(p[0], v -> open(p.length > 1 ? p[1] : p[0]));
            x.setOnLongClickListener(v -> { L.remove(s); store(k, L, crypt); list(k, lim, crypt); return true; });
        }
        t("Нажмите и удерживайте, чтобы убрать.", 12, false, Color.GRAY);
        bt("Добавить чат", v -> {
            final EditText e = new EditText(this);
            e.setHint("@username или ссылка t.me/...");
            new AlertDialog.Builder(this).setView(e).setPositiveButton("Добавить", (d, w) -> {
                String u = e.getText().toString().trim().replace("https://t.me/", "").replace("@", "");
                if (u.length() == 0) return;
                if (L.size() >= max) { toast("Лимит " + lim + ". Снимается в VPrime"); return; }
                L.add(u + "|" + u);
                store(k, L, crypt);
                list(k, lim, crypt);
            }).show();
        });
        bt("Назад", v -> main());
    }

    void skey() {
        final boolean first = !sp.contains("skey_salt");
        final EditText e = new EditText(this);
        e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        e.setHint(first ? "Придумайте пароль sKey" : "Пароль sKey");
        new AlertDialog.Builder(this).setTitle("sKey").setView(e).setPositiveButton("OK", (d, w) -> {
            try {
                String pw = e.getText().toString();
                if (pw.length() < 4) { toast("Минимум 4 символа"); return; }
                byte[] salt;
                if (first) {
                    salt = new byte[16];
                    new SecureRandom().nextBytes(salt);
                    sp.edit().putString("skey_salt", Base64.encodeToString(salt, Base64.NO_WRAP)).apply();
                    key = derive(pw, salt);
                    sp.edit().putString("skey_chk", encS("swgram")).apply();
                } else {
                    salt = Base64.decode(sp.getString("skey_salt", ""), Base64.NO_WRAP);
                    key = derive(pw, salt);
                    if (!"swgram".equals(decS(sp.getString("skey_chk", "")))) throw new Exception("bad");
                }
                list("skey", 750, true);
            } catch (Exception x) { key = null; toast("Неверный пароль"); }
        }).setNegativeButton("Отмена", null).show();
    }

    void icons() {
        begin("Иконка и название");
        t("Бесплатно: 3 цвета и 2 названия. VPrime: больше.", 13, false, Color.GRAY);
        String[][] free = {{"blue", "Голубая"}, {"red", "Красная"}, {"gold", "Золотая"}};
        String[][] pr = {{"purple", "Фиолетовая"}, {"green", "Зелёная"}, {"pink", "Розовая"}};
        String[] nf = {"swgram", "miligram"};
        String[] np = {"kilogram", "maxagram", "pashagram"};
        for (String[] c : free) for (String n : nf) iconBtn(c[0], c[1], n, false);
        for (String[] c : pr) iconBtn(c[0], c[1], "swgram", true);
        for (String n : np) iconBtn("blue", "Голубая", n, true);
        bt("Назад", v -> main());
    }

    void iconBtn(final String c, String cn, final String n, final boolean p) {
        bt(n + " - " + cn + (p ? " (VPrime)" : ""), v -> {
            if (p && !isPrime()) { toast("Нужен VPrime"); return; }
            setIcon("A_" + c + "_" + n);
        });
    }

    void setIcon(String a) {
        try {
            PackageManager pm = getPackageManager();
            PackageInfo pi = pm.getPackageInfo(getPackageName(), PackageManager.GET_ACTIVITIES | PackageManager.GET_DISABLED_COMPONENTS);
            String want = "org.nqe.sw." + a;
            for (ActivityInfo ai : pi.activities) {
                if (ai.targetActivity == null) continue;
                pm.setComponentEnabledSetting(new ComponentName(getPackageName(), ai.name), ai.name.equals(want) ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED : PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
            }
            toast("Готово, значок обновится через несколько секунд");
        } catch (Exception e) { toast("Не удалось сменить значок"); }
    }

    class Plane extends View {
        Bitmap b;
        float rx, ry, lx, ly;
        boolean down;
        Camera cm = new Camera();
        Matrix m = new Matrix();

        Plane(Context c) {
            super(c);
            int id = c.getResources().getIdentifier("sw_blue", "drawable", c.getPackageName());
            b = BitmapFactory.decodeResource(c.getResources(), id);
        }

        public boolean onTouchEvent(MotionEvent e) {
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN: down = true; lx = e.getX(); ly = e.getY(); getParent().requestDisallowInterceptTouchEvent(true); break;
                case MotionEvent.ACTION_MOVE: ry += (e.getX() - lx) * 0.6f; rx -= (e.getY() - ly) * 0.6f; lx = e.getX(); ly = e.getY(); break;
                default: down = false;
            }
            invalidate();
            return true;
        }

        protected void onDraw(Canvas c) {
            if (b == null) return;
            if (!down) ry += 0.8f;
            cm.save(); cm.rotateX(rx); cm.rotateY(ry); cm.getMatrix(m); cm.restore();
            float s = Math.min(getWidth(), getHeight()) * 0.8f / b.getWidth();
            m.preTranslate(-b.getWidth() / 2f, -b.getHeight() / 2f);
            m.postScale(s, s);
            m.postTranslate(getWidth() / 2f, getHeight() / 2f);
            c.drawBitmap(b, m, null);
            postInvalidateOnAnimation();
        }
    }

    void primeScreen() {
        begin("VPrime");
        root.addView(new Plane(this), new LinearLayout.LayoutParams(-1, dp(240)));
        t(isPrime() ? "Активна до " + new java.util.Date(sp.getLong("prime", 0)) : "Не активна", 14, false, Color.GRAY);
        t("Возможности: быстрые чаты без лимита (250), sKey без лимита (750), все цвета значка и названия, градиентные обои.", 14, false, Color.WHITE);
        bt("Купить за 50 звёзд", v -> buy());
        bt("Назад", v -> main());
    }

    void buy() {
        new AlertDialog.Builder(this).setTitle("VPrime на 3 месяца")
            .setMessage("Звёзды уходят владельцу при оплате подписки. Отправьте подарок за 50 звёзд аккаунту @youtubeskuf, затем нажмите «Я оплатил».")
            .setPositiveButton("Открыть чат владельца", (d, w) -> open("youtubeskuf"))
            .setNeutralButton("Я оплатил", (d, w) -> {
                sp.edit().putLong("prime", System.currentTimeMillis() + 90L * 86400000L).apply();
                toast("VPrime активирован на 3 месяца");
                primeScreen();
            })
            .setNegativeButton("Отмена", null).show();
    }

    void plugins() {
        begin("Плагины");
        final ArrayList<String> L = load("plugins", false);
        for (final String s : L) {
            String[] p = s.split("\\|", 2);
            final String code = p.length > 1 ? new String(Base64.decode(p[1], Base64.NO_WRAP)) : "";
            Button x = bt(p[0] + "  (JS)", v -> runJs(code));
            x.setOnLongClickListener(v -> { L.remove(s); store("plugins", L, false); plugins(); return true; });
        }
        t("Нажмите, чтобы запустить. Удерживайте, чтобы удалить.", 12, false, Color.GRAY);
        bt("Добавить вручную", v -> addPlugin(L));
        bt("Назад", v -> main());
    }

    void addPlugin(final ArrayList<String> L) {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        final EditText n = new EditText(this);
        final EditText c = new EditText(this);
        n.setHint("Название");
        c.setHint("Скрипт (JavaScript)");
        c.setMinLines(8);
        c.setTypeface(Typeface.MONOSPACE);
        c.setGravity(Gravity.TOP);
        l.addView(n);
        l.addView(c);
        new AlertDialog.Builder(this).setTitle("Терминал").setView(l).setPositiveButton("Сохранить", (d, w) -> {
            if (n.getText().length() == 0) return;
            L.add(n.getText().toString().replace("|", "/").replace("\n", " ") + "|" + Base64.encodeToString(c.getText().toString().getBytes(), Base64.NO_WRAP));
            store("plugins", L, false);
            plugins();
        }).setNegativeButton("Отмена", null).show();
    }

    void runJs(final String code) {
        final WebView w = new WebView(this);
        w.getSettings().setJavaScriptEnabled(true);
        w.loadData("<html></html>", "text/html", "utf-8");
        w.postDelayed(() -> w.evaluateJavascript(code, r -> toast("Результат: " + r)), 400);
    }

    void funcs() {
        begin("Функции sw");
        t("Работает:\n- Ghost: чтение, истории, набор текста, онлайн\n- Запрет скриншотов\n- Желейные кнопки\n- Быстрые чаты без лимита\n- sKey с паролем и шифрованием списка\n- Смена значка и названия\n- VPrime (экран и активация)\n- Плагины на JavaScript\n\nВ разработке:\n- Переписка по Bluetooth\n- Автоответчик\n- Команды .qr и .gs\n- Размытие номера и юзернейма\n- Локальный ИИ и бот @ai\n- Голос в текст\n- Безопасное соединение (VPN)\n- Точное время в сети", 14, false, Color.WHITE);
        bt("Назад", v -> main());
    }
}
