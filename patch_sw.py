import re,sys
J='src/TMessagesProj/src/main/java/org/telegram/'
def sub(path,pat,fn,count=0):
    s=open(path,encoding='utf-8').read()
    s,n=re.subn(pat,fn,s,count=count)
    open(path,'w',encoding='utf-8').write(s)
    return n
a=sub(J+'messenger/ApplicationLoader.java',r'(public void onCreate\(\)\s*\{)',lambda m:m.group(1)+'\n        org.nqe.sw.SwCore.init(this);',1)
b=sub(J+'tgnet/ConnectionsManager.java',r'(public int sendRequest\((?:final\s+)?TLObject\s+(\w+)[^)]*\)\s*\{)',lambda m:m.group(1)+'\n        if (org.nqe.sw.SwCore.block('+m.group(2)+')) return 0;')
print('hooks',a,b)
M='src/TMessagesProj/src/main/AndroidManifest.xml'
s=open(M,encoding='utf-8').read()
if a==0 or b==0 or 'org.telegram.ui.LaunchActivity' not in s:
    print('FAIL ApplicationLoader',a,'ConnectionsManager',b,'LaunchActivity','org.telegram.ui.LaunchActivity' in s)
    sys.exit(1)
cols=['blue','red','gold','purple','green','pink'];names=['swgram','miligram','kilogram','maxagram','pashagram']
combos=[(c,n) for c in cols[:3] for n in names[:2]]+[(c,'swgram') for c in cols[3:]]+[('blue',n) for n in names[2:]]
x='<activity android:name="org.nqe.sw.SwSettingsActivity" android:label="swgram sw" android:icon="@drawable/sw_blue" android:theme="@android:style/Theme.Material.NoActionBar" android:exported="true"><intent-filter><action android:name="android.intent.action.MAIN"/><category android:name="android.intent.category.LAUNCHER"/></intent-filter></activity>'
for c,n in combos:
    x+='<activity-alias android:name="org.nqe.sw.A_%s_%s" android:targetActivity="org.telegram.ui.LaunchActivity" android:label="%s" android:icon="@drawable/sw_%s" android:enabled="false" android:exported="true"><intent-filter><action android:name="android.intent.action.MAIN"/><category android:name="android.intent.category.LAUNCHER"/></intent-filter></activity-alias>'%(c,n,n,c)
i=s.rindex('</application>')
open(M,'w',encoding='utf-8').write(s[:i]+x+s[i:])
