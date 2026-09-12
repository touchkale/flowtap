#!/usr/bin/env python3
"""Deterministic, dependency-free SVG mockup generator. Run from any directory."""
from pathlib import Path
from html import escape
import math, textwrap, json

ROOT = Path(__file__).resolve().parent
LIGHT = dict(bg='#F5F4EE', card='#FFFFFF', ink='#192D28', muted='#596A63', line='#DCE3DC', accent='#245C48', soft='#E4EDE3', on='#FFFFFF', danger='#A43D35')
DARK = dict(bg='#101B18', card='#1B2A24', ink='#F0F4ED', muted='#B0C2B6', line='#34473C', accent='#ACD6B0', soft='#2B4234', on='#142B1F', danger='#FFB4AA')
SIZES = [('compact',360,800),('pixel-10-pro',400,892.5),('large',440,956)]

def rect(x,y,w,h,fill,rx=0,stroke='none'):
    return f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="{rx}" fill="{fill}" stroke="{stroke}"/>'
def txt(x,y,s,size=16,color='#192D28',weight=400,anchor='start'):
    return f'<text x="{x}" y="{y}" font-size="{size}" font-weight="{weight}" text-anchor="{anchor}" fill="{color}">{escape(str(s))}</text>'
def circle(x,y,r,fill,stroke='none',sw=1):
    return f'<circle cx="{x}" cy="{y}" r="{r}" fill="{fill}" stroke="{stroke}" stroke-width="{sw}"/>'
def icon(name,x,y,color,size=24):
    paths = {
      'home':'M3 11 12 3 21 11 M5 10v11h5v-7h4v7h5V10',
      'history':'M3 11a9 9 0 1 1 2 7 M3 4v7h7 M12 7v6l4 2',
      'tag':'M3 3h8l10 10-8 8L3 11Z M7 7h.01',
      'modes':'M4 6h16 M4 12h16 M4 18h16 M8 3v6 M16 9v6 M10 15v6',
      'settings':'M9 3h6l1 4 4 2v6l-4 2-1 4H9l-1-4-4-2V9l4-2Z M15 12a3 3 0 1 1-6 0 3 3 0 0 1 6 0',
      'check':'M5 12l5 5L20 6','arrow':'M8 4l8 8-8 8','back':'M16 4l-8 8 8 8',
      'plus':'M12 4v16 M4 12h16','lock':'M6 10h12v11H6Z M8 10V7a4 4 0 0 1 8 0v3',
      'leaf':'M5 20C1 9 8 3 21 3c0 13-7 18-16 17Zm0 0L16 8',
      'scan':'M4 8V4h4 M16 4h4v4 M20 16v4h-4 M8 20H4v-4 M9 8v8 M13 6q7 6 0 12',
      'search':'M17 10a7 7 0 1 1-14 0 7 7 0 0 1 14 0 M15 15l6 6',
      'shield':'M12 2 3 6v6q1 7 9 10 8-3 9-10V6Z M8 12l3 3 5-6',
      'bell':'M5 16h14l-2-3V9a5 5 0 0 0-10 0v4Z M10 20h4',
      'more':'M5 12h.01 M12 12h.01 M19 12h.01',
      'sun':'M16 12a4 4 0 1 1-8 0 4 4 0 0 1 8 0 M12 1v3 M12 20v3 M1 12h3 M20 12h3 M4 4l2 2 M18 18l2 2 M4 20l2-2 M18 6l2-2',
      'moon':'M20 15A9 9 0 0 1 9 3a9 9 0 1 0 11 12',
      'phone':'M7 2h10v20H7Z M10 18h4',
    }
    return f'<g transform="translate({x} {y}) scale({size/24})" fill="none" stroke="{color}" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"><path d="{paths.get(name,paths["leaf"])}"/></g>'

class Screen:
    def __init__(self,key,title,subtitle='',nav=None,dark=False,w=400,h=892.5,back=False,step=None):
        self.key,self.title,self.w,self.h,self.nav=key,title,w,h,nav
        self.p=DARK if dark else LIGHT; self.body=[]; self.chrome=[]; self.y=0; self.extent=0
        self.cw=w-40; self.top=164 if step else 146
        p=self.p
        self.chrome += [rect(0,0,w,h,p['bg'],30),txt(25,29,'9:41',13,p['ink'],600),circle(w/2,23,5,'#17211D')]
        for i in range(4): self.chrome.append(rect(w-82+i*5,26-i*3,3,4+i*3,p['ink'],1))
        self.chrome += [rect(w-51,17,25,12,'none',3,p['ink']),rect(w-48,20,18,6,p['ink'],1),rect(w-24,21,2,4,p['ink'],1)]
        if back: self.chrome.append(icon('back',20,63,p['ink'])); tx=56
        else: tx=20
        self.chrome += [txt(tx,87,title,28 if len(title)<18 else 24,p['ink'],650),txt(20,116,subtitle,14,p['muted'])]
        if step:
            for i in range(3): self.chrome.append(rect(20+i*((w-40)/3),136,(w-52)/3,4,p['accent'] if i<step else p['line'],2))
        self.bottom=h-(110 if nav else 100)
    def add(self,s): self.body.append(s)
    def text(self,s,size=16,color=None,weight=400,x=20,dy=None):
        self.add(txt(x,self.y+size,s,size,color or self.p['ink'],weight)); self.y+=dy or size+12
    def para(self,s,size=14,x=20,width=None,color=None):
        width=width or self.w-x-20
        for line in textwrap.wrap(s,max(12,int(width/(size*.52)))):
            self.add(txt(x,self.y+size,line,size,color or self.p['muted'])); self.y+=size+7
        self.y+=7
    def section(self,s):
        self.y+=12; self.text(s,13,self.p['muted'],650,dy=32)
    def card(self,title,sub='',ico='leaf',tail='arrow',selected=False):
        p=self.p; lines=textwrap.wrap(sub,max(12,int((self.cw-100)/7))) if sub else []
        ht=max(76,50+len(lines)*19)
        self.add(rect(20,self.y,self.cw,ht,p['soft'] if selected else p['card'],18,p['accent'] if selected else p['line']))
        self.add(rect(34,self.y+18,36,36,p['soft'],12)); self.add(icon(ico,41,self.y+25,p['accent'],22))
        self.add(txt(82,self.y+30,title,16,p['ink'],600))
        for j,line in enumerate(lines): self.add(txt(82,self.y+52+j*19,line,13,p['muted']))
        if tail=='on' or tail=='off':
            self.add(rect(self.w-69,self.y+23,35,22,p['accent'] if tail=='on' else p['line'],11)); self.add(circle(self.w-(46 if tail=='on' else 57),self.y+34,8,p['on'] if tail=='on' else p['card']))
        elif tail: self.add(icon(tail,self.w-57,self.y+26,p['accent'],20))
        self.y+=ht+10
    def banner(self,title,sub='',warning=False):
        p=self.p; start=self.y; lines=textwrap.wrap(sub,int((self.cw-36)/7))
        ht=48+len(lines)*20
        self.add(rect(20,start,self.cw,ht,p['soft'],16))
        self.add(txt(36,start+28,title,15,p['danger'] if warning else p['accent'],650))
        for i,line in enumerate(lines): self.add(txt(36,start+51+i*20,line,13,p['muted']))
        self.y+=ht+14
    def field(self,label,value):
        self.text(label,13,self.p['muted'],600,dy=27)
        self.add(rect(20,self.y,self.cw,54,self.p['card'],12,self.p['line'])); self.add(txt(36,self.y+34,value,16,self.p['ink'])); self.y+=70
    def chips(self,labels,chosen=0):
        gap=8; cw=(self.cw-gap*(len(labels)-1))/len(labels)
        for i,label in enumerate(labels):
            self.add(rect(20+i*(cw+gap),self.y,cw,48,self.p['accent'] if i==chosen else self.p['card'],14,self.p['line']))
            self.add(txt(20+i*(cw+gap)+cw/2,self.y+30,label,14,self.p['on'] if i==chosen else self.p['muted'],600,'middle'))
        self.y+=62
    def hero(self,kind='scan'):
        p=self.p; cx=self.w/2; cy=self.y+105
        for r in [96,73,50]: self.add(circle(cx,cy,r,p['soft'] if r==96 else 'none',p['line']))
        self.add(rect(cx-29,cy-42,58,84,p['card'],14,p['accent'])); self.add(icon(kind,cx-18,cy-20,p['accent'],36)); self.y+=224
    def ring(self,active=False,total='48m',caption='of your 2h daily goal',progress=.4):
        p=self.p; start=self.y; cx=self.w/2; cy=start+154; r=94
        self.add(rect(20,start,self.cw,302,p['card'],24,p['line']))
        self.add(txt(40,start+32,'SESSION IN PROGRESS' if active else 'TODAY’S FOCUS',12,p['accent'],650))
        self.add(circle(cx,cy,r,'none',p['line'],9))
        self.add(f'<circle cx="{cx}" cy="{cy}" r="{r}" fill="none" stroke="{p["accent"]}" stroke-width="9" stroke-linecap="round" stroke-dasharray="{2*math.pi*r*progress} {2*math.pi*r}" transform="rotate(-90 {cx} {cy})"/>')
        self.add(txt(cx,cy+5,total,44,p['ink'],600,'middle')); self.add(txt(cx,cy+31,'Deep Work' if active else 'focused today',14,p['muted'],400,'middle'))
        self.add(txt(cx,start+277,caption,14,p['muted'],400,'middle')); self.y+=316
    def button(self,label,secondary=False):
        p=self.p; self.chrome += [rect(0,self.bottom,self.w,self.h-self.bottom,p['bg']),rect(20,self.bottom+10,self.cw,54,p['card'] if secondary else p['accent'],17,p['line'] if secondary else 'none'),txt(self.w/2,self.bottom+44,label,16,p['ink'] if secondary else p['on'],650,'middle')]
    def finish(self,offset=0,button=None,secondary=False,nav=None):
        p=self.p; nav=nav or self.nav
        end=self.bottom if button else self.h-(110 if nav else 36)
        cid=self.key.replace('.','-')+'-clip'
        full=''.join(self.chrome)+f'<defs><clipPath id="{cid}"><rect x="0" y="{self.top}" width="{self.w}" height="{end-self.top}"/></clipPath></defs>'
        full+=f'<g clip-path="url(#{cid})"><g transform="translate(0 {self.top-offset})">'+''.join(self.body)+'</g></g>'
        if self.y-offset>end-self.top:
            full+=rect(self.w-7,self.top+8,3,end-self.top-20,p['line'],2)
            full+=rect(self.w-7,self.top+8+min(offset/self.y,.8)*(end-self.top-80),3,58,p['muted'],2)
        if button:
            self.chrome=[]; self.button(button,secondary); full+=''.join(self.chrome)
        if nav:
            ny=self.h-100
            full+=rect(0,ny,self.w,100,p['card'])+rect(0,ny,self.w,1,p['line'])
            for i,name in enumerate(['Home','History','Tags','Modes','Settings']):
                cx=self.w*(i+.5)/5
                if name==nav: full+=rect(cx-27,ny+10,54,30,p['soft'],15)
                full+=icon(name.lower(),cx-11,ny+14,p['accent'] if name==nav else p['muted'],22)
                full+=txt(cx,ny+61,name,11,p['accent'] if name==nav else p['muted'],650 if name==nav else 400,'middle')
        full+=rect(self.w/2-53,self.h-13,106,4,p['ink'],2)
        return full

def make(key,w=400,h=892.5,dark=False,full=False):
    opts={}; title='FlowTap'; sub='Make room for what matters.'; nav=None; back=False; step=None; button=None; secondary=False; offset=0
    if key.startswith('onboard'):
        step=int(key[-1]); title=['Pair your first tag','Set up your focus','Choose your apps'][step-1]; sub=f'Step {step} of 3'; button=['Scan NFC tag','Continue','Start using FlowTap'][step-1]
    elif key.startswith('home') or key=='active': nav='Home'
    elif key.startswith('history'): title='Your focus, over time'; sub='Small sessions. Meaningful progress.'; nav='History'
    elif key.startswith('tag'): title='Your tags'; sub='A physical shortcut to being present.'; nav='Tags'
    elif key.startswith('mode') and not key.startswith('editor'): title='Focus modes'; sub='Different moments. Your own rules.'; nav='Modes'
    elif key.startswith('editor'): title='Edit mode'; sub='Deep Work · Changes apply next session'; back=True; button='Save changes'
    elif key.startswith('settings'): title='Settings'; sub='Make FlowTap work for you.'; nav='Settings'
    elif key=='blocked': title='FlowTap'; sub='Deep Work is protecting this moment.'; button='Back to focus'
    elif key=='notification': title='Sat, 12 Sep'; sub='Notification preview · Android';
    elif key=='emergency': title='End this session?'; sub='Emergency access'; back=True; button='Hold for 3 seconds to end'; secondary=True
    if key=='editor-create': title='Create mode'; sub='Make space for a different kind of focus.'; button='Create mode'
    s=Screen(key,title,sub,nav,dark,w,h,back,step); p=s.p
    if key=='onboard1':
        s.hero(); s.text('A small tap. A fresh start.',24,weight=600); s.para('Pair a tag, place it where you focus, and tap your phone to begin.')
        s.field('FIRST MODE','Deep Work'); s.para('No tag yet? You can start from the app.'); s.chips(['Set up later'], -1)
    elif key=='onboard2':
        s.banner('You’re in control','Each permission opens Android settings. FlowTap does not read your content.')
        s.card('App blocking','Required · Return from distracting apps','shield',tail='check',selected=True)
        s.card('NFC scanning','Required for tag shortcuts','scan',tail='check')
        s.card('Notifications','Optional · See your session timer','bell')
        s.card('Do Not Disturb','Optional · Silence interruptions','moon')
        s.card('Background sessions','Keep your session running','phone')
        s.card('Uninstall protection','Optional · Prevent impulsive removal','lock')
    elif key=='onboard3' or key=='editor-apps':
        s.banner('Choose what can wait','Only selected apps will be blocked during Deep Work.')
        s.field('SEARCH APPS','Search installed apps'); s.chips(['All apps','Selected (3)'],1)
        for name,sel in [('Instagram',True),('YouTube',True),('Reddit',True),('Chrome',False),('Messages',False)]: s.card(name,'Selected' if sel else 'Available during focus','phone','check' if sel else 'plus',sel)
        if key=='editor-apps': button='Done · 3 apps selected'
    elif key in ['home-idle','home-ended','active','home-active']:
        active=key in ['active','home-active']; ended=key=='home-ended'
        s.card('Deep Work','3 apps blocked · Silent','leaf','lock' if active else 'arrow')
        s.ring(active,'24:08' if active else ('1h 13m' if ended else '48m'),'48m today + this session' if active else ('47m left to your 2h goal' if ended else '1h 12m left to your 2h goal'),.61 if ended else (.6 if active else .4))
        s.banner('Session saved · 25 minutes' if ended else ('Your focus is protected' if active else 'Ready when you are'), 'Keep the momentum, or take a break.' if ended else ('Tap your starting tag to finish.' if active else 'Tap your Desk tag or start below.'))
        if active:
            s.section('SESSION OPTIONS'); s.card('Emergency end','Press and hold for 3 seconds','lock')
        else:
            s.section('RECENT SESSIONS'); s.card('Deep Work','Today, 08:30 · 48 min','history')
        button='Hold 3s for emergency end' if active else 'Start focus'; secondary=active
        # Home body reserves primary action + navigation instead of covering content.
        s.bottom=h-174
    elif key=='history-list':
        s.chips(['This week','Last week'],0); s.text('6h 49m',40,weight=600); s.para('Focused this week · 12 sessions')
        start=s.y; s.add(rect(20,start,s.cw,205,p['card'],22,p['line']))
        vals=[40,72,55,92,77,73,0]; barw=(s.cw-48)/7
        for i,v in enumerate(vals):
            x=44+i*barw; s.add(rect(x,start+145-v,barw-12,max(v,3),p['accent'] if i==5 else p['soft'],5)); s.add(txt(x+(barw-12)/2,start+176,'MTWTFSS'[i],12,p['muted'],400,'middle'))
        s.y+=221; s.section('TODAY · SATURDAY, 12 SEP')
        s.card('Deep Work','09:41–10:06 · 25 min','history'); s.card('Deep Work','08:30–09:18 · 48 min','history'); s.section('YESTERDAY'); s.card('Reading','20:00–20:35 · 35 min','history')
    elif key in ['history-detail','history-delete']:
        s.banner('Deep Work','Saturday, 12 September'); s.text('25 minutes',38,weight=600); s.para('09:41–10:06 · Completed')
        for a,b,c in [('Started with','Desk tag','tag'),('Ended with','Desk tag','check'),('Protection','3 apps blocked · Strict mode','shield')]: s.card(a,b,c,tail=None)
        s.section('SESSION RECORD')
        if key=='history-delete':
            s.banner('Delete this session?','This removes 25 minutes from your history and daily total. This cannot be undone.',True); s.chips(['Keep session','Delete'], -1)
        else: s.card('Delete session','You’ll confirm before it is removed','history')
    elif key=='tag-empty':
        s.hero('tag'); s.text('Your first focus shortcut',23,weight=600); s.para('Keep a tag on your desk, by a book, or wherever you want fewer distractions.'); s.banner('One tag. One focus mode.','Tap to start. Tap again to finish.'); button='Pair a tag'; s.bottom=h-174
    elif key=='tag-pair':
        s.text('Pair a new tag',25,weight=600); s.field('TAG NAME','Desk'); s.section('CHOOSE A MODE'); s.card('Deep Work','3 apps blocked · 2h daily goal','leaf','check',True); s.card('Reading','2 apps blocked · 30m daily goal','moon'); s.hero('scan'); s.para('Hold a tag near the back of your phone.'); button='Scan NFC tag'; s.bottom=h-174
    elif key=='tag-success':
        s.banner('Desk tag paired','Your tag is ready for Deep Work.'); s.section('YOUR TAGS'); s.card('Desk','Deep Work · Tap to edit','tag'); s.para('To change what this tag starts, edit its mode.'); button='Pair another tag'; s.bottom=h-174
    elif key=='tag-error':
        s.hero('scan'); s.text('Couldn’t read that tag',24,weight=600); s.para('Keep the tag still against the back of your phone and try again.'); s.banner('NFC turned off?','Turn NFC on in Android settings, then return here.'); button='Try again'; s.bottom=h-174
    elif key in ['mode-list','mode-menu','mode-saved']:
        if key=='mode-saved': s.banner('Mode saved','Deep Work is ready for your next session.')
        for a,b,c in [('Deep Work','3 blocked · Silent · Strict','leaf'),('Reading','2 blocked · Silent · Flexible','moon'),('Offline evening','5 blocked · DND · Strict','sun')]:
            s.card(a,b,c,'more'); s.para('Daily goal: '+('2 hours' if a=='Deep Work' else '30 minutes'),13,x=34)
        if key=='mode-menu':
            s.section('DEEP WORK'); s.card('Edit mode','Change apps and session rules','modes'); s.card('Duplicate mode','Create a copy to customize','plus'); s.card('Delete mode','Confirm before deleting','history')
        button='Create mode'; s.bottom=h-174
    elif key.startswith('editor'):
        s.field('MODE NAME','e.g. Study time' if key=='editor-create' else 'Deep Work'); s.section('DAILY GOAL'); s.chips(['30 min','1 hour','2 hours'],2); s.card('Custom goal','Choose hours and minutes','history')
        s.section('APPS'); s.chips(['Block selected','Allow selected'],0); s.card('Selected apps','No apps selected' if key=='editor-create' else 'Instagram, YouTube, Reddit','phone'); s.para('Choose apps to block during this mode.' if key=='editor-create' else 'Only these 3 apps are blocked. Everything else stays available.')
        s.section('SESSION RULES'); s.card('Strict mode','End with the starting tag. Emergency hold stays available.','lock','on')
        s.section('SOUND'); s.chips(['Unchanged','Silent','DND'],1); s.para('Your original sound settings return when the session ends.')
        s.section('ADVANCED'); s.card('Auto-end timer','Off · Choose 5–720 minutes','history'); s.card('Keep screen awake','Use app default','sun'); s.card('Launch app on start','None','phone')
        s.section('LINKED TAGS'); s.card('Desk','Paired with Deep Work','tag','check',True); s.para('Tag assignments update when you save.'); s.banner('Ready to save','3 apps blocked · 2h goal · Silent · Strict')
        if key=='editor-bottom': offset=max(0,s.y-(s.bottom-s.top))
    elif key.startswith('settings'):
        s.banner('Ready to protect your focus','All required permissions are enabled.'); s.section('BLOCKING METHOD'); s.card('Full power','Instant blocking · Recommended','shield','check',True); s.card('Compatibility','About 2s delay · Easier on banking apps','phone')
        s.para('Some banking apps may reject accessibility access. Try Compatibility if that happens.')
        s.section('PERMISSIONS & PROTECTION')
        for a,b,c in [('App blocking','Granted · Accessibility enabled','shield'),('Usage access','Granted · Used by Compatibility','phone'),('Draw over apps','Granted · Block screen available','phone'),('Do Not Disturb access','Granted · Follows your mode','moon'),('Notifications','Granted · Session updates','bell'),('Uninstall protection','Off · Optional protection','lock')]: s.card(a,b,c)
        s.section('BEHAVIOR'); s.card('Use Do Not Disturb','Only for modes using DND','moon','on'); s.card('Hardcore mode','Locks settings and the normal End action. End by tag; emergency hold remains.','lock','off'); s.card('Keep screen awake','During focus sessions','sun','off')
        s.section('APPEARANCE'); s.chips(['System','Light','Dark'],2 if dark else 0)
        s.section('ABOUT'); s.card('FlowTap v0.1.0','On-device only. No accounts.','leaf'); s.card('Uninstall FlowTap','Opens Android’s confirmation screen','phone')
        if key=='settings-middle': offset=490
        if key=='settings-bottom': offset=max(0,s.y-(s.h-110-s.top))
    elif key=='blocked':
        s.hero('leaf'); s.text('Let YouTube wait.',29,weight=600); s.para('You chose this time for Deep Work. Your next small step is still there.'); s.banner('24 minutes of focus','YouTube is blocked until this session ends.'); s.card('End with your Desk tag','Strict mode · Use the starting tag','tag',None); s.card('Need to end urgently?','Press and hold for emergency access','lock')
    elif key=='emergency':
        s.hero('lock'); s.text('A deliberate way out.',27,weight=600); s.para('Hold the button for 3 seconds to end Deep Work. Release to cancel.'); s.banner('Your time will be saved','Blocked apps and sound settings will be restored.'); s.card('Keep focusing','Return to your session','back')
    elif key=='notification':
        s.chips(['Wi-Fi','Bluetooth','DND'],2); s.y+=36; s.add(rect(20,s.y,s.cw,206,p['card'],24,p['line'])); start=s.y
        s.add(icon('leaf',38,start+21,p['accent'])); s.add(txt(72,start+39,'FlowTap · now',14,p['muted'])); s.add(txt(38,start+80,'Deep Work is running',20,p['ink'],600)); s.add(txt(38,start+108,'24:08 elapsed · 3 apps blocked',14,p['muted'])); s.add(rect(38,start+135,s.cw-36,49,p['soft'],14)); s.add(txt(w/2,start+165,'Return to session',15,p['accent'],600,'middle')); s.y+=230; s.para('Tap your Desk tag when you’re ready to finish.')
    else: raise ValueError(key)
    if full:
        s.h=s.y+s.top+(184 if nav and button else 112)
        s.bottom=s.h-(174 if nav and button else 100)
        s.chrome.insert(0,rect(0,0,w,s.h,p['bg']))
        offset=0
    return s.finish(offset,button,secondary),s.y

WORKFLOWS=[
('01-onboarding','A quieter start','Pair a tag → enable protection → choose distractions', [('onboard1','01 · Pair or skip'),('onboard2','02 · Permissions'),('onboard3','03 · App selection')]),
('02-home-session','Make focus the main event','Ready → protected session → saved progress', [('home-idle','01 · Ready to start'),('home-active','02 · Strict session running'),('home-ended','03 · Session completed')]),
('03-history','Progress without pressure','A readable week, a useful record, a deliberate delete', [('history-list','01 · Weekly overview'),('history-detail','02 · Session details'),('history-delete','03 · Confirm deletion')]),
('04-tags-nfc','A physical shortcut to focus','Empty state → pairing → success, with a clear retry path', [('tag-empty','01 · First tag'),('tag-pair','02 · Name, mode, scan'),('tag-success','03 · Paired'),('tag-error','04 · Scan recovery')]),
('05-modes','Rules for different moments','Scan your modes, manage one, see what changed', [('mode-list','01 · Mode library'),('mode-menu','02 · Mode actions'),('editor-create','03 · Create a mode'),('mode-saved','04 · Save confirmation')]),
('06-mode-editor','Power, progressively revealed','One scrolling form · stable Save action · beginning and end shown', [('editor-top','01 · Form / start'),('editor-apps','02 · App picker / subview'),('editor-bottom','03 · Form / scrolled to end')]),
('07-settings','Clear status. Less guesswork.','One scrolling page · readiness first · preferences grouped by purpose', [('settings-top','01 · Readiness / start'),('settings-middle','02 · Permissions / middle'),('settings-bottom','03 · Behavior / end')]),
('08-blocking-overlay','Protect the moment, calmly','A useful interruption with a visible emergency route', [('active','01 · Focus running'),('notification','02 · System notification'),('blocked','03 · Blocked app'),('emergency','04 · Emergency end')]),
('09-theming','The same calm, after dark','Matched hierarchy and geometry across light and dark', [('home-idle','01 · Light Home',False),('home-idle','02 · Dark Home',True),('mode-list','03 · Dark Modes',True),('settings-bottom','04 · Dark Settings',True)])]

def svg(w,h,body,title,desc):
    if w<=440 and h<1000:
        body=f'<defs><clipPath id="phone-outline"><rect width="{w}" height="{h}" rx="30"/></clipPath></defs><g clip-path="url(#phone-outline)">{body}</g>'
    return f'<svg xmlns="http://www.w3.org/2000/svg" width="{w}" height="{h}" viewBox="0 0 {w} {h}" role="img" aria-labelledby="title desc"><title id="title">{escape(title)}</title><desc id="desc">{escape(desc)}</desc><g font-family="Arial, Helvetica, sans-serif">{body}</g></svg>\n'

def main():
    (ROOT/'screens').mkdir(exist_ok=True)
    manifest=[]
    for slug,title,desc,panels in WORKFLOWS:
        cols=2 if len(panels)==4 else 3; rows=math.ceil(len(panels)/cols); bw=cols*448+64; bh=190+rows*1010+108
        body=rect(0,0,bw,bh,'#EAEDE5')+txt(48,45,'FLOWTAP / DESIGN STUDY 02',13,'#526553',650)+txt(48,93,title,36,'#192D28',650)+txt(48,127,desc,17,'#596A63')
        for i,panel in enumerate(panels):
            key,label=panel[:2]; dark=panel[2] if len(panel)>2 else False
            x=48+(i%cols)*448; y=183+(i//cols)*1010
            content,_=make(key,dark=dark)
            body+=txt(x,y,label,16,'#245C48',600)+rect(x-5,y+22,410,902.5,'#273830',35)+f'<g transform="translate({x} {y+27})">{content}</g>'
            body+=txt(x,y+957,'400 × 892.5 logical canvas · Pixel 10 Pro',12,'#596A63')
            for size,w,h in SIZES:
                content,extent=make(key,w,h,dark)
                filename=f'{key}{"-dark" if dark else ""}-{size}.svg'
                (ROOT/'screens'/filename).write_text(svg(w,h,content,label,desc+'; static vector design proposal.'))
                manifest.append(dict(workflow=slug,file='screens/'+filename,width=w,height=h))
        body+=txt(48,bh-64,'VECTOR PROPOSAL · Sample data · Android app behavior requires implementation and device testing.',14,'#596A63')
        body+=txt(48,bh-37,'Compact 360 × 800 / Pixel 400 × 892.5 / Large 440 × 956 · Individual SVGs and layout notes in the gallery.',14,'#596A63')
        (ROOT/(slug+'.svg')).write_text(svg(bw,bh,body,title,desc))
    # A dedicated side-by-side composition exposes actual reflow instead of shrinking a screenshot.
    body=rect(0,0,1376,1170,'#EAEDE5')+txt(40,65,'One layout. Three phone sizes.',34,weight=650)+txt(40,99,'Fixed type and touch sizes. Flexible cards. Short screens scroll.',17,'#596A63')
    x=40
    for size,w,h in SIZES:
        content,_=make('home-idle',w,h)
        body+=txt(x,147,f'{size} · {w} × {h}',15,'#245C48',600)+f'<g transform="translate({x} 170)">{content}</g>'; x+=w+48
    (ROOT/'10-responsive-check.svg').write_text(svg(1376,1170,body,'Responsive phone comparison','Compact, Pixel 10 Pro and larger phone geometry with fixed text sizes.'))
    for key in ['editor-top','settings-top']:
        for size,w,h in SIZES:
            content,extent=make(key,w,h,full=True)
            height=extent+146+112
            (ROOT/'screens'/f'{key}-continuous-{size}.svg').write_text(svg(w,height,content,key+' / continuous scroll content','Expanded content sheet; not physical device height.'))
    (ROOT/'manifest.json').write_text(json.dumps(manifest,indent=2)+'\n')
    gallery='''<!doctype html><html lang="en"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>FlowTap — UI design studies</title><style>*{box-sizing:border-box}body{margin:0;background:#eaeDE5;color:#192d28;font:16px/1.5 system-ui,sans-serif}header,main{max-width:1480px;margin:auto;padding:32px}h1{font-size:clamp(32px,5vw,58px);line-height:1.1;letter-spacing:-2px;margin:16px 0}p{max-width:780px;color:#596a63}a{color:#245c48}nav{display:flex;gap:12px;flex-wrap:wrap}nav a,select{padding:12px 16px;border:1px solid #bacaba;border-radius:12px;background:#f5f4ee}section{margin:48px 0}h2{font-size:26px;margin-bottom:4px}.screens{display:flex;gap:24px;align-items:flex-start;overflow:auto;padding:20px 4px}figure{margin:0;flex:0 0 auto}figure img{display:block;border:5px solid #273830;border-radius:35px;max-width:calc(100vw - 72px);height:auto}figcaption{font-size:14px;margin:0 0 12px}small{display:block;color:#596a63}label{display:block;margin:24px 0}footer{padding:32px}a:focus-visible,select:focus-visible{outline:3px solid #245c48;outline-offset:4px}</style><header><small>FLOWTAP / EDITABLE VECTOR DESIGN STUDY</small><h1>Less noise.<br>More room to focus.</h1><p>A calm, consistent redesign of all nine app workflows. Review each phone at its natural size, compare compact and larger layouts, or open the complete SVG boards. These are static design proposals with illustrative session data.</p><nav>'''
    for slug,title,desc,panels in WORKFLOWS: gallery+=f'<a href="#{slug}">{escape(slug[3:].replace("-"," ").title())}</a>'
    gallery+='</nav><label>Phone canvas <select id="size"><option value="pixel-10-pro">Pixel 10 Pro · 400 × 892.5</option><option value="compact">Compact · 360 × 800</option><option value="large">Large · 440 × 956</option></select></label><a href="10-responsive-check.svg">Compare three sizes</a> · <a href="DESIGN.md">Layout and behavior notes</a></header><main>'
    for slug,title,desc,panels in WORKFLOWS:
        gallery+=f'<section id="{slug}"><h2>{escape(title)}</h2><p>{escape(desc)} · <a href="{slug}.svg">Open SVG board</a></p><div class="screens">'
        for panel in panels:
            key,label=panel[:2]; dark=panel[2] if len(panel)>2 else False; stem=key+('-dark' if dark else '')
            gallery+=f'<figure><figcaption>{escape(label)}</figcaption><a data-stem="{stem}" href="screens/{stem}-pixel-10-pro.svg"><img data-stem="{stem}" src="screens/{stem}-pixel-10-pro.svg" width="400" alt="{escape(label)} — {escape(title)}" loading="lazy"></a></figure>'
        gallery+='</div>'
        if slug in ['06-mode-editor','07-settings']:
            key='editor-top' if slug=='06-mode-editor' else 'settings-top'
            gallery+=f'<p><a href="screens/{key}-continuous-pixel-10-pro.svg">Open full continuous scrolling content</a></p>'
        gallery+='</section>'
    gallery+='''</main><footer>Original screenshots are preserved in <a href="reference/README.md">reference/</a>. Mockups do not change the Android app.</footer><script>document.querySelector('#size').addEventListener('change',e=>{const size=e.target.value;const widths={compact:360,'pixel-10-pro':400,large:440};document.querySelectorAll('[data-stem]').forEach(el=>{const src=`screens/${el.dataset.stem}-${size}.svg`;if(el.tagName==='IMG'){el.src=src;el.width=widths[size]}else el.href=src})})</script></html>'''
    (ROOT/'index.html').write_text(gallery)
    print(f'Generated {len(WORKFLOWS)} workflow boards, responsive comparison and {len(set(m["file"] for m in manifest))} phone SVGs.')

if __name__=='__main__': main()
