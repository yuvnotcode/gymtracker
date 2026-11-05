const $ = (selector) => document.querySelector(selector);
const state = { token: localStorage.getItem('gymtracker.token'), user: JSON.parse(localStorage.getItem('gymtracker.user') || 'null'), mode: 'login', workouts: [], draft: null, splitType: 'FULL_BODY' };
const splitPlans = {
  PUSH_PULL_LEGS: ['Push', 'Pull', 'Legs'],
  UPPER_LOWER: ['Upper', 'Lower'],
  BRO_SPLIT: ['Chest', 'Back', 'Shoulders', 'Arms', 'Legs'],
  FULL_BODY: ['Full Body A', 'Full Body B', 'Full Body C']
};
const today = new Date();
const dateInput = (date) => `${date.getFullYear()}-${String(date.getMonth()+1).padStart(2,'0')}-${String(date.getDate()).padStart(2,'0')}`;
$('#welcome-date').textContent = today.toLocaleDateString(undefined,{weekday:'long',month:'long',day:'numeric'}).toUpperCase();

function showApp() {
  const signedIn = Boolean(state.token && state.user);
  document.body.classList.toggle('training-mode', signedIn);
  document.body.classList.toggle('auth-mode', !signedIn);
  $('#auth-view').hidden = signedIn;
  $('#dashboard-view').hidden = !signedIn;
  if (!signedIn) { state.draft=null; $('#sidebar-name').textContent='Your account';$('#avatar').textContent='Y';updateDraftBanner(); return; }
  try { state.draft=JSON.parse(localStorage.getItem(draftKey())||'null'); } catch { state.draft=null; }
  updateDraftBanner();
  const name = state.user.name || state.user.email.split('@')[0];
  const initial = name.charAt(0).toUpperCase();
  $('#welcome-name').textContent = name.split(' ')[0];
  $('#sidebar-name').textContent = name;
  $('#avatar').textContent = initial;
  loadSplit();
  loadWorkouts();
}
async function api(path, options = {}) {
  const response = await fetch(path, { ...options, headers: { 'Content-Type': 'application/json', ...(state.token ? { Authorization: `Bearer ${state.token}` } : {}), ...(options.headers || {}) } });
  if (response.status === 401) { logout(); throw new Error('Your session expired. Please sign in again.'); }
  if (!response.ok) { let message = `Request failed (${response.status})`; try { const body = await response.json(); message = body.error || body.message || body.detail || body.title || message; } catch {} throw new Error(message); }
  if (response.status === 204) return null;
  const body = await response.text();
  return body.trim() ? JSON.parse(body) : null;
}
function logout() { stopRestTimer(''); state.token = null; state.user = null; state.workouts = []; state.splitType = 'FULL_BODY'; localStorage.removeItem('gymtracker.token'); localStorage.removeItem('gymtracker.user'); showApp(); }
$('#logout').addEventListener('click', logout);
$('#logout-mobile').addEventListener('click', logout);

document.querySelectorAll('.auth-tab').forEach(button => button.addEventListener('click', () => {
  state.mode = button.dataset.mode;
  document.querySelectorAll('.auth-tab').forEach(tab => tab.classList.toggle('active', tab === button));
  $('#register-fields').hidden = state.mode !== 'register';
  $('#auth-submit').innerHTML = state.mode === 'register' ? 'Create account <span>→</span>' : 'Sign in <span>→</span>';
  $('#auth-form [name="password"]').autocomplete = state.mode === 'register' ? 'new-password' : 'current-password';
  $('#auth-error').textContent = '';
}));
$('#auth-form').addEventListener('submit', async event => {
  event.preventDefault(); const authForm = event.currentTarget; const form = new FormData(authForm); const payload = Object.fromEntries(form.entries());
  $('#auth-error').textContent = ''; const submit = $('#auth-submit'); submit.disabled = true;
  try {
    if (state.mode === 'register') {
      if (!payload.age) delete payload.age; else payload.age = Number(payload.age);
      if (!payload.currentWeight) delete payload.currentWeight; else payload.currentWeight = Number(payload.currentWeight);
      const registered = await api('/users/register', { method: 'POST', body: JSON.stringify(payload) });
      state.user = registered;
    }
    const session = await api('/users/login', { method: 'POST', body: JSON.stringify({ email: payload.email, password: payload.password }) });
    state.token = session.token;
    state.user = { id: session.id, name: session.name, email: session.email };
    localStorage.setItem('gymtracker.token', state.token); localStorage.setItem('gymtracker.user', JSON.stringify(state.user)); authForm.reset(); showApp();
  } catch (error) { $('#auth-error').textContent = error.message; }
  finally { submit.disabled = false; }
});

function renderWorkouts() {
  const list = $('#workout-list'); list.replaceChildren();
  const grouped = new Map();
  state.workouts.forEach(item => { const key = `${item.workoutDate}|${item.workoutName}`; if (!grouped.has(key)) grouped.set(key, []); grouped.get(key).push(item); });
  const sessions = [...grouped.entries()].sort((a,b) => b[0].localeCompare(a[0]));
  $('#empty-state').hidden = sessions.length > 0;
  $('#entry-count').textContent = `${sessions.length} ${sessions.length === 1 ? 'session' : 'sessions'}`;
  sessions.forEach(([key, entries]) => {
    const [date, title] = key.split('|'); const d = new Date(`${date}T12:00:00`); const volume = entries.reduce((sum,item) => sum + (item.setDetails?.length ? item.setDetails.reduce((subtotal,set) => subtotal + set.reps * set.weight,0) : item.sets * item.reps * item.weight), 0);
    const row = document.createElement('article'); row.className = 'session-card';
    const summary = document.createElement('div'); summary.className='session-summary';
    const dateBox = document.createElement('div'); dateBox.className = 'workout-date'; dateBox.innerHTML = `<strong>${d.getDate()}</strong><span>${d.toLocaleDateString(undefined,{month:'short'}).toUpperCase()}</span>`;
    const main = document.createElement('div'); main.className = 'workout-main'; const heading = document.createElement('strong'); heading.textContent = title; const detail = document.createElement('small'); detail.textContent = `${entries.length} exercise${entries.length===1?'':'s'} · tap to open session`; main.append(heading,detail);
    const metric = document.createElement('div'); metric.className = 'workout-metric'; metric.innerHTML = `<strong>${entries.length} exercise${entries.length===1?'':'s'}</strong><small>${Math.round(volume).toLocaleString()} kg volume</small>`;
    const actions = document.createElement('div'); const del = document.createElement('button'); del.className = 'delete-entry'; del.title = `Delete ${title} session`; del.setAttribute('aria-label',del.title); del.textContent = '×'; del.addEventListener('click', () => deleteSession(entries,title)); actions.append(del);
    const open = document.createElement('button');open.className='session-open';open.type='button';open.setAttribute('aria-expanded','false');open.setAttribute('aria-label',`Open ${title} workout from ${date}`);open.textContent='＋';
    summary.append(dateBox,main,metric,open,actions);
    const details=document.createElement('div');details.className='session-details';details.hidden=true;
    entries.forEach(item=>{const exercise=document.createElement('div');exercise.className='session-exercise';const name=document.createElement('strong');name.textContent=item.exerciseName;const setBlock=document.createElement('div');setBlock.className='session-set-list';if(item.setDetails?.length){item.setDetails.forEach((set,index)=>{const chip=document.createElement('span');chip.className='session-set';chip.textContent=`SET ${set.setNumber||index+1}  ·  ${set.reps} REPS  ·  ${set.weight} KG${set.rir==null?'':`  ·  ${set.rir} RIR`}`;setBlock.append(chip);});}else{const chip=document.createElement('span');chip.className='session-set';chip.textContent=`${item.sets} SETS  ·  ${item.reps} REPS EACH  ·  ${item.weight} KG RECORDED LOAD`;setBlock.append(chip);}exercise.append(name,setBlock);details.append(exercise);});
    open.addEventListener('click',()=>{const expanded=!details.hidden;details.hidden=expanded;open.setAttribute('aria-expanded',String(!expanded));open.textContent=expanded?'＋':'−';row.classList.toggle('expanded',!expanded);});
    row.append(summary,details); list.append(row);
  });
  const weekStart = new Date(today); weekStart.setHours(0,0,0,0); weekStart.setDate(today.getDate() - ((today.getDay()+6)%7));
  const weekEntries = state.workouts.filter(item => new Date(`${item.workoutDate}T12:00:00`) >= weekStart && new Date(`${item.workoutDate}T12:00:00`) <= today);
  const weekSessions = new Set(weekEntries.map(item => `${item.workoutDate}|${item.workoutName}`));
  $('#week-count').textContent = weekSessions.size;
  const totalVolume = weekEntries.reduce((sum,item) => sum + (item.setDetails?.length ? item.setDetails.reduce((subtotal,set) => subtotal + set.reps*set.weight,0) : item.sets*item.reps*item.weight),0);
  $('#volume-value').textContent = Math.round(totalVolume).toLocaleString();
  $('#volume-rule').style.width = `${Math.min(100,Math.log10(1+totalVolume)*27)}%`;
  const days = new Set(weekEntries.map(item => item.workoutDate)); const dots = $('#week-dots'); dots.replaceChildren(); for(let i=0;i<7;i++){const day=new Date(weekStart);day.setDate(weekStart.getDate()+i);const dot=document.createElement('span');if(days.has(dateInput(day)))dot.className='done';dots.append(dot);}
  let streak=0; const cursor=new Date(today); if(!days.has(dateInput(cursor)))cursor.setDate(cursor.getDate()-1); while(state.workouts.some(item=>item.workoutDate===dateInput(cursor))){streak++;cursor.setDate(cursor.getDate()-1);if(streak>366)break;}
  $('#streak-value').textContent=streak;$('#streak-track').style.width=`${Math.min(100,streak*14)}%`;
  renderProgress();
  renderSplit();
}
async function loadWorkouts() { try { state.workouts = await api(`/users/${state.user.id}/workouts`); renderWorkouts(); } catch(error) { console.error(error); } }
async function deleteWorkout(id) { try { await api(`/workouts/${id}`, {method:'DELETE'}); state.workouts = state.workouts.filter(item=>item.id!==id); renderWorkouts(); } catch(error) { window.alert(error.message); } }
async function deleteSession(entries,name) { if(!window.confirm(`Delete the ${name} session and all its exercises?`))return;try{const query=new URLSearchParams({ids:entries.map(item=>item.id).join(',')});await api(`/users/${state.user.id}/workouts/session?${query}`,{method:'DELETE'});const ids=new Set(entries.map(item=>item.id));state.workouts=state.workouts.filter(item=>!ids.has(item.id));renderWorkouts();}catch(error){window.alert(error.message);} }

const svgNS='http://www.w3.org/2000/svg';
function svgNode(tag,attributes={}){const node=document.createElementNS(svgNS,tag);Object.entries(attributes).forEach(([key,value])=>node.setAttribute(key,value));return node;}
function renderProgress(){
  const select=$('#progress-exercise');if(!select)return;
  const previous=select.value;const exerciseMap=new Map();
  state.workouts.forEach(item=>{const key=(item.exerciseName||'').trim().toLocaleLowerCase();if(!key)return;if(!exerciseMap.has(key))exerciseMap.set(key,{name:item.exerciseName.trim(),sets:[]});const exercise=exerciseMap.get(key);const sets=item.setDetails?.length?item.setDetails:[{reps:item.reps,weight:item.weight}];sets.forEach(set=>exercise.sets.push({date:item.workoutDate,weight:Number(set.weight)||0,reps:Number(set.reps)||0}));});
  const exercises=[...exerciseMap.entries()].sort((a,b)=>a[1].name.localeCompare(b[1].name));select.replaceChildren();
  if(!exercises.length){const empty=document.createElement('option');empty.value='';empty.textContent='Log workouts to see progress';select.append(empty);$('#progress-latest').textContent='—';$('#progress-best').textContent='—';$('#progress-sessions').textContent='0';$('#chart-date-range').textContent='NO SESSIONS YET';$('#progress-chart').replaceChildren();return;}
  exercises.forEach(([key,exercise])=>{const option=document.createElement('option');option.value=key;option.textContent=exercise.name;select.append(option);});
  select.value=exercises.some(([key])=>key===previous)?previous:exercises[0][0];drawProgress(exerciseMap.get(select.value));
}
function drawProgress(exercise){
  const chart=$('#progress-chart');chart.replaceChildren();if(!exercise)return;
  const byDate=new Map();exercise.sets.forEach(set=>{if(!byDate.has(set.date)||set.weight>byDate.get(set.date).weight||(set.weight===byDate.get(set.date).weight&&set.reps>byDate.get(set.date).reps))byDate.set(set.date,set);});
  const points=[...byDate.values()].sort((a,b)=>a.date.localeCompare(b.date));const best=points.reduce((max,p)=>p.weight>max.weight||p.weight===max.weight&&p.reps>max.reps?p:max,points[0]);const latest=points.at(-1);
  const label=set=>`${Number.isInteger(set.weight)?set.weight:set.weight.toFixed(1)} kg × ${set.reps}`;
  $('#progress-latest').textContent=label(latest);$('#progress-best').textContent=label(best);$('#progress-sessions').textContent=points.length;
  $('#chart-date-range').textContent=points.length===1?new Date(`${points[0].date}T12:00:00`).toLocaleDateString(undefined,{month:'short',year:'numeric'}):`${new Date(`${points[0].date}T12:00:00`).toLocaleDateString(undefined,{month:'short',year:'2-digit'})} — ${new Date(`${points.at(-1).date}T12:00:00`).toLocaleDateString(undefined,{month:'short',year:'2-digit'})}`;
  const W=800,H=230,left=52,right=785,top=24,bottom=190;for(let i=0;i<4;i++){const y=top+(bottom-top)*i/3;chart.append(svgNode('line',{x1:left,y1:y,x2:right,y2:y,class:'chart-grid'}));}
  const min=Math.min(...points.map(p=>p.weight)),max=Math.max(...points.map(p=>p.weight)),pad=(max-min)*.25||Math.max(max*.12,5);const low=Math.max(0,min-pad),high=max+pad;
  const coords=points.map((p,i)=>({x:points.length===1?(left+right)/2:left+(right-left)*i/(points.length-1),y:bottom-(p.weight-low)/(high-low)*(bottom-top),p}));
  if(points.length>1){chart.append(svgNode('polyline',{points:coords.map(p=>`${p.x},${p.y}`).join(' '),class:'chart-line'}));}
  coords.forEach(({x,y,p},i)=>{const circle=svgNode('circle',{cx:x,cy:y,r:i===coords.length-1?6:4,class:i===coords.length-1?'chart-point current':'chart-point'});const title=svgNode('title');title.textContent=`${p.date}: ${label(p)}`;circle.append(title);chart.append(circle);});
  const last=coords.at(-1);const value=svgNode('text',{x:last.x,y:Math.max(15,last.y-14),'text-anchor':last.x>W-120?'end':'middle',class:'chart-value'});value.textContent=`${last.p.weight} KG`;chart.append(value);
}
function nextSplitDay(){
  const plan=splitPlans[state.splitType]||splitPlans.FULL_BODY;
  const sessions=[...new Map(state.workouts.map(item=>[`${item.workoutDate}|${item.workoutName}`,{date:item.workoutDate,name:item.workoutName}])).values()].sort((a,b)=>a.date.localeCompare(b.date));
  const normalize=value=>(value||'').toLowerCase().replace(/[^a-z0-9]/g,'');
  for(let i=sessions.length-1;i>=0;i--){const name=normalize(sessions[i].name);const index=plan.findIndex(day=>name.includes(normalize(day))||normalize(day).includes(name));if(index>=0)return plan[(index+1)%plan.length];}
  return plan[0];
}
function renderSplit(){
  const select=$('#split-select');if(!select)return;
  if(select.value!==state.splitType)select.value=state.splitType;
  const plan=splitPlans[state.splitType]||splitPlans.FULL_BODY;const next=nextSplitDay();$('#next-workout-name').textContent=next;
  const days=$('#split-days');days.replaceChildren();plan.forEach((day,index)=>{const item=document.createElement('div');item.className=`split-day${day===next?' is-next':''}`;const number=document.createElement('span');number.textContent=String(index+1).padStart(2,'0');const name=document.createElement('strong');name.textContent=day;const tag=document.createElement('small');tag.textContent=day===next?'UP NEXT':'IN ROTATION';item.append(number,name,tag);days.append(item);});
}
async function loadSplit(){try{const result=await api('/users/me/split');state.splitType=result.splitType||'FULL_BODY';$('#split-select').value=state.splitType;$('#split-status').textContent='';renderSplit();}catch(error){$('#split-status').textContent='Could not load your split. Refresh to try again.';}}
$('#save-split').addEventListener('click',async()=>{const button=$('#save-split');const previous=state.splitType;const requested=$('#split-select').value;button.disabled=true;$('#split-status').textContent='Saving your plan…';state.splitType=requested;renderSplit();try{const result=await api('/users/me/split',{method:'PUT',body:JSON.stringify({splitType:requested})});state.splitType=result.splitType;$('#split-status').textContent='Saved to your account';renderSplit();}catch(error){state.splitType=previous;$('#split-status').textContent=error.message;renderSplit();}finally{button.disabled=false;}});
$('#progress-exercise').addEventListener('change',event=>{const item=state.workouts.find(workout=>workout.exerciseName?.trim().toLocaleLowerCase()===event.target.value);if(item)drawProgress({name:item.exerciseName,sets:state.workouts.filter(workout=>workout.exerciseName?.trim().toLocaleLowerCase()===event.target.value).flatMap(workout=>(workout.setDetails?.length?workout.setDetails:[{reps:workout.reps,weight:workout.weight}]).map(set=>({date:workout.workoutDate,weight:Number(set.weight)||0,reps:Number(set.reps)||0})))});});

const navigationLinks=[...document.querySelectorAll('.side-nav .nav-item')];
const navigationTargets=navigationLinks.map(link=>({link,target:document.querySelector(link.getAttribute('href'))})).filter(item=>item.target);
function setActiveNavigation(targetId){navigationTargets.forEach(({link,target})=>{const active=target.id===targetId;link.classList.toggle('active',active);if(active)link.setAttribute('aria-current','location');else link.removeAttribute('aria-current');});}
let navigationLock=null;
function updateActiveNavigation(){if(!document.body.classList.contains('training-mode')||navigationLock)return;const marker=window.innerHeight*.24;const passed=navigationTargets.map(({target})=>({target,top:target.getBoundingClientRect().top})).filter(item=>item.top<=marker).sort((a,b)=>a.top-b.top);const current=passed.at(-1)?.target||navigationTargets[0]?.target;if(current)setActiveNavigation(current.id);}
navigationLinks.forEach(link=>link.addEventListener('click',event=>{const target=document.querySelector(link.getAttribute('href'));if(!target)return;event.preventDefault();navigationLock=target.id;setActiveNavigation(target.id);history.replaceState(null,'',link.getAttribute('href'));target.scrollIntoView({behavior:'smooth',block:'start'});window.setTimeout(()=>{navigationLock=null;},900);}));
window.addEventListener('scroll',updateActiveNavigation,{passive:true});window.addEventListener('resize',updateActiveNavigation);window.addEventListener('hashchange',updateActiveNavigation);

const modal = $('#workout-modal');
let restRemaining=90,restInterval=null;
function formatRestTime(seconds){return `${String(Math.floor(seconds/60)).padStart(2,'0')}:${String(seconds%60).padStart(2,'0')}`;}
function renderRestTimer(){
  $('#rest-time').textContent=formatRestTime(restRemaining);
  $('#rest-start').textContent=restInterval?'Pause timer':'Start rest';
  $('#rest-time').classList.toggle('is-finished',restRemaining===0);
}
function stopRestTimer(status){if(restInterval){clearInterval(restInterval);restInterval=null;}if(status)$('#rest-status').textContent=status;renderRestTimer();}
$('#rest-duration').addEventListener('change',event=>{stopRestTimer('Rest interval updated');restRemaining=Number(event.target.value);renderRestTimer();});
$('#rest-start').addEventListener('click',()=>{
  if(restInterval){stopRestTimer('Rest paused');return;}
  if(restRemaining===0)restRemaining=Number($('#rest-duration').value);
  $('#rest-status').textContent='Resting';renderRestTimer();
  restInterval=window.setInterval(()=>{restRemaining=Math.max(0,restRemaining-1);renderRestTimer();if(restRemaining===0){stopRestTimer('Rest complete · ready for your next set');if(navigator.vibrate)navigator.vibrate([180,100,180]);}},1000);
  renderRestTimer();
});
$('#rest-reset').addEventListener('click',()=>{stopRestTimer('Timer reset');restRemaining=Number($('#rest-duration').value);renderRestTimer();});
function draftKey(){const email=state.user?.email?.trim().toLowerCase();return `gymtracker.draft.${encodeURIComponent(email||'account')}`;}
function updateDraftBanner(){const banner=$('#active-workout-banner');if(!banner)return;const visible=Boolean(state.token&&state.user&&state.draft);banner.hidden=!visible;if(!visible){$('#start-workout').innerHTML='＋ <span>Start a workout</span>';return;}const name=state.draft.workoutName?.trim()||'WORKOUT IN PROGRESS';const count=state.draft.exercises?.length||0;$('#active-workout-name').textContent=name.toUpperCase();$('#active-workout-meta').textContent=`${count} exercise${count===1?'':'s'} · draft saved on this device`;$('#start-workout').innerHTML='↻ <span>Resume workout</span>';}
function captureDraft(){const form=$('#workout-form');if(!form)return null;const values=Object.fromEntries(new FormData(form).entries());return{workoutName:values.workoutName||'',workoutDate:values.workoutDate||dateInput(today),exercises:[...$('#exercise-list').children].map(card=>({exerciseName:card.querySelector('[name="exerciseName"]').value||'',sets:[...card.querySelectorAll('.set-row')].map(row=>({reps:row.querySelector('[name="reps"]').value,weight:row.querySelector('[name="weight"]').value,rir:row.querySelector('[name="rir"]').value}))}))};}
function saveDraft(){if(!state.user)return;const draft=captureDraft();if(!draft)return;state.draft=draft;localStorage.setItem(draftKey(),JSON.stringify(draft));updateDraftBanner();}
function restoreDraft(draft){$('#workout-form').reset();$('#workout-form [name="workoutName"]').value=draft.workoutName||'';$('#workout-form [name="workoutDate"]').value=draft.workoutDate||dateInput(today);$('#exercise-list').replaceChildren();(draft.exercises?.length?draft.exercises:[{}]).forEach(exercise=>addExercise(exercise.exerciseName||'',exercise.sets||[]));}
function clearDraft(){if(state.user)localStorage.removeItem(draftKey());state.draft=null;updateDraftBanner();$('#start-workout').innerHTML='＋ <span>Start a workout</span>';}
function addSetRow(container, values={reps:10,weight:0,rir:''}) {
  const row=document.createElement('div');row.className='set-row';
  const count=document.createElement('span');count.className='set-count';count.textContent=container.children.length+1;
  for(const [label,name,value,attrs] of [['Reps','reps',values.reps,'min="1" max="500"'],['kg','weight',values.weight,'min="0" max="2000" step="0.5"'],['RIR','rir',values.rir,'min="0" max="10"']]){const wrap=document.createElement('label');wrap.textContent=label;const input=document.createElement('input');input.type='number';input.name=name;input.value=value;input.required=name!=='rir';input.setAttribute('aria-label',label);input.setAttribute('min',name==='rir'?'0':name==='reps'?'1':'0');if(name==='weight')input.step='0.5';if(name==='rir')input.max='10';if(name==='reps')input.max='500';wrap.append(input);row.append(wrap);}
  const remove=document.createElement('button');remove.type='button';remove.className='remove-set';remove.textContent='×';remove.title='Remove set';remove.addEventListener('click',()=>{if(container.children.length>1){row.remove();[...container.children].forEach((el,i)=>el.querySelector('.set-count').textContent=i+1);saveDraft();}});row.prepend(count);row.append(remove);container.append(row);
}
function addExercise(name='', setValues=[]) {
  const card=document.createElement('article');card.className='exercise-card dynamic-exercise';
  const header=document.createElement('div');header.className='dynamic-exercise-heading';const title=document.createElement('strong');title.textContent=`Exercise ${$('#exercise-list').children.length+1}`;const remove=document.createElement('button');remove.type='button';remove.className='remove-set';remove.textContent='Remove';remove.addEventListener('click',()=>{if($('#exercise-list').children.length>1){card.remove();[...$('#exercise-list').children].forEach((el,i)=>el.querySelector('.dynamic-exercise-heading strong').textContent=`Exercise ${i+1}`);saveDraft();}});header.append(title,remove);
  const label=document.createElement('label');label.textContent='Exercise name';const input=document.createElement('input');input.name='exerciseName';input.required=true;input.maxLength=100;input.placeholder='e.g. Barbell squat';input.value=name;label.append(input);
  const setsHead=document.createElement('div');setsHead.className='sets-label-row';setsHead.innerHTML='<span>SETS</span><span>REPS</span><span>LOAD (KG)</span><span>RIR</span><span></span>';
  const sets=document.createElement('div');sets.className='set-list';(setValues.length?setValues:[{reps:10,weight:0,rir:''},{reps:10,weight:0,rir:''},{reps:10,weight:0,rir:''}]).forEach(set=>addSetRow(sets,set));
  const add=document.createElement('button');add.type='button';add.className='add-set';add.textContent='＋ Add set';add.addEventListener('click',()=>{addSetRow(sets,{reps:10,weight:0,rir:''});saveDraft();});card.append(header,label,setsHead,sets,add);$('#exercise-list').append(card);saveDraft();
}
function openModal() { if(state.draft)restoreDraft(state.draft);else{$('#workout-form').reset();$('#workout-form [name="workoutDate"]').value=dateInput(today);$('#workout-form [name="workoutName"]').value=nextSplitDay();$('#exercise-list').replaceChildren();addExercise();}$('#workout-error').textContent='';modal.hidden=false;saveDraft();$('#workout-form [name="workoutName"]').focus(); }
function closeModal() { saveDraft();modal.hidden=true; }
$('#start-workout').addEventListener('click',openModal);$('#empty-start').addEventListener('click',openModal);document.querySelectorAll('.close-modal').forEach(button=>button.addEventListener('click',closeModal));modal.addEventListener('click',event=>{if(event.target===modal)closeModal();});document.addEventListener('keydown',event=>{if(event.key==='Escape')closeModal();});
$('#add-exercise').addEventListener('click',()=>addExercise());$('#workout-form').addEventListener('input',saveDraft);$('#workout-form').addEventListener('change',saveDraft);$('#resume-workout').addEventListener('click',openModal);$('#discard-workout').addEventListener('click',()=>{if(window.confirm('Discard this unfinished workout draft?'))clearDraft();});
$('#workout-form').addEventListener('submit',async event=>{
  event.preventDefault(); const base=Object.fromEntries(new FormData(event.currentTarget).entries());const exercises=[...$('#exercise-list').children].map(card=>({exerciseName:card.querySelector('[name="exerciseName"]').value.trim(),sets:[...card.querySelectorAll('.set-row')].map(row=>({reps:Number(row.querySelector('[name="reps"]').value),weight:Number(row.querySelector('[name="weight"]').value),rir:row.querySelector('[name="rir"]').value===''?null:Number(row.querySelector('[name="rir"]').value)}))})); const button=$('#save-workout');button.disabled=true;$('#workout-error').textContent='';
  try { const saved=await api(`/users/${state.user.id}/workouts/session`,{method:'POST',body:JSON.stringify({...base,exercises})});state.workouts.unshift(...saved);renderWorkouts();clearDraft();stopRestTimer('');restRemaining=Number($('#rest-duration').value);renderRestTimer();modal.hidden=true; }catch(error){$('#workout-error').textContent=error.message;}finally{button.disabled=false;}
});
showApp();
