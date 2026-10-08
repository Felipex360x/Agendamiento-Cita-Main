/* Nicolett Studio Fantasy - lógica compartida (mockup, datos en localStorage).
   Para conectar con Spring Boot, reemplaza las funciones de "API" por fetch('/api/...'). */
const SERVICES=[
 {id:'cabello',name:'Corte & styling',category:'Cabello',description:'Un corte que te representa, un acabado que enamora.',duration:60,price:25000,image:'img/hair.jpg'},
 {id:'color',name:'Color & balayage',category:'Coloración',description:'Luz, dimensión y el tono perfecto para ti.',duration:120,price:65000,image:'img/hair.jpg'},
 {id:'unas',name:'Manicure & cuidado',category:'Uñas',description:'Los pequeños detalles también hablan de ti.',duration:45,price:18000,image:'img/nails.jpg'},
 {id:'tratamiento',name:'Tratamiento capilar',category:'Bienestar',description:'Un momento de pausa para renovar tu cabello.',duration:60,price:35000,image:'img/salon-interior.jpg'}];
const SPECIALISTS=[{id:'camila',name:'Camila Rojas',initials:'CR',specialty:'Colorista & estilista'},{id:'valentina',name:'Valentina Soto',initials:'VS',specialty:'Estilista & cuidado capilar'},{id:'isidora',name:'Isidora Muñoz',initials:'IM',specialty:'Manicurista'}];
const DEPOSIT=5000;
const money=n=>'$'+new Intl.NumberFormat('es-CL').format(n);
const $=s=>document.querySelector(s);
const SEED=[{id:'1',client:'Antonia Silva',service:'Color & balayage',specialist:'camila',date:'2026-10-06',time:'10:00',status:'Confirmada'},{id:'2',client:'Francisca Pérez',service:'Corte & styling',specialist:'camila',date:'2026-10-07',time:'11:00',status:'Confirmada'},{id:'3',client:'Catalina Torres',service:'Tratamiento capilar',specialist:'valentina',date:'2026-10-08',time:'09:00',status:'Confirmada'},{id:'4',client:'Antonia Silva',service:'Corte & styling',specialist:'camila',date:'2026-10-09',time:'15:00',status:'Confirmada'},{id:'5',client:'Javiera Díaz',service:'Manicure & cuidado',specialist:'isidora',date:'2026-10-06',time:'12:00',status:'Confirmada'}];
const store={
 get appointments(){return JSON.parse(localStorage.getItem('ns_appts')||'null')||SEED},
 set appointments(v){localStorage.setItem('ns_appts',JSON.stringify(v))},
 get notes(){return JSON.parse(localStorage.getItem('ns_notes')||'{}')},
 set notes(v){localStorage.setItem('ns_notes',JSON.stringify(v))},
 add(a){this.appointments=[...this.appointments,a]},
 cancel(id){this.appointments=this.appointments.map(a=>a.id===id?{...a,status:'Anulada'}:a)}
};

function renderShell(){
 const page=document.body.dataset.page;
 const l=(h,t,p)=>`<a href="${h}" class="nav-link ${page===p?'active':''}">${t}</a>`;
 $('#header').outerHTML=`<header class="site-header"><div class="container"><a href="index.html" class="wordmark">nicolett<span>.</span><small>STUDIO FANTASY</small></a><nav class="main">${l('index.html','Inicio','home')}${l('servicios.html','Servicios','servicios')}${l('equipo.html','Nuestro equipo','equipo')}</nav><div class="row"><a href="agenda.html" class="nav-link hide-m">Acceso profesionales</a><a href="reservar.html" class="btn sm">Reservar hora</a></div></div></header>`;
 $('#footer').outerHTML=`<footer class="footer"><div class="container row between"><span class="display" style="font-size:24px;color:var(--fg)">nicolett.</span><span>Un espacio para ti. Un cuidado que se siente.</span><span>© 2026 Nicolett Studio Fantasy · Buin, Chile</span></div></footer>`;
 const s=$('#staffnav'); if(s) s.outerHTML=`<div class="staffnav"><div class="container row between"><div class="row" style="gap:24px">${l('agenda.html','Agenda semanal','agenda')}${l('clientas.html','Historial de clientas','clientas')}${l('dashboard.html','Perfil demostrativo','dashboard')}</div><span class="small">Espacio profesionales · Datos de muestra</span></div></div>`;
}
const serviceCard=s=>`<article class="card"><img src="${s.image}" alt="${s.name}" loading="lazy"><div class="body"><span class="eyebrow">${s.category}</span><h3>${s.name}</h3><p class="small">${s.description}</p><div class="row between" style="margin-top:16px;padding-top:14px;border-top:1px solid var(--border)"><span>Desde <strong>${money(s.price)}</strong> · <span class="small">${s.duration} min</span></span><a class="btn sm outline" href="reservar.html?servicio=${s.id}">Reservar</a></div></div></article>`;

/* ---------- Páginas ---------- */
const pages={
 home(){ $('#services').innerHTML=SERVICES.slice(0,3).map(serviceCard).join('') },
 servicios(){ $('#services').innerHTML=SERVICES.map(serviceCard).join('') },
 equipo(){ $('#team').innerHTML=SPECIALISTS.map(p=>`<div class="panel row"><div class="avatar">${p.initials}</div><div><h3 style="font-size:24px">${p.name}</h3><p class="small">${p.specialty}</p></div></div>`).join('') },

 reservar(){
  const st={step:1,service:new URLSearchParams(location.search).get('servicio'),specialist:null,date:null,time:null,name:'',email:'',phone:'',paid:false};
  if(st.service)st.step=2;
  const titles=['Elige tu servicio','Elige a tu especialista','Elige la fecha','Elige la hora','Revisa tu reserva','Abono para reservar','¡Reserva confirmada!'];
  const days=[...Array(10)].map((_,i)=>{const d=new Date();d.setDate(d.getDate()+i+1);return d}).filter(d=>d.getDay()!==0);
  const hours=['09:00','10:00','11:00','12:00','15:00','16:00','17:00','18:00'];
  const iso=d=>d.toISOString().slice(0,10);
  const taken=()=>store.appointments.filter(a=>a.specialist===st.specialist&&a.date===st.date&&a.status==='Confirmada').map(a=>a.time);
  const svc=()=>SERVICES.find(s=>s.id===st.service), sp=()=>SPECIALISTS.find(s=>s.id===st.specialist);
  function draw(){
   const s=st.step; let h='';
   if(s===1)h=`<div class="grid g2">${SERVICES.map(x=>`<button class="choice ${st.service===x.id?'selected':''}" data-k="service" data-v="${x.id}"><span><strong>${x.name}</strong><br><span class="small">${x.duration} min</span></span><strong>${money(x.price)}</strong></button>`).join('')}</div>`;
   if(s===2)h=`<div class="grid g3">${SPECIALISTS.map(x=>`<button class="choice ${st.specialist===x.id?'selected':''}" data-k="specialist" data-v="${x.id}"><span class="row"><span class="avatar">${x.initials}</span><span><strong>${x.name}</strong><br><span class="small">${x.specialty}</span></span></span></button>`).join('')}</div>`;
   if(s===3)h=`<div class="grid g4">${days.map(d=>`<button class="choice ${st.date===iso(d)?'selected':''}" data-k="date" data-v="${iso(d)}"><span>${d.toLocaleDateString('es-CL',{weekday:'long',day:'numeric',month:'short'})}</span></button>`).join('')}</div>`;
   if(s===4){const t=taken();h=`<div class="grid g4">${hours.map(x=>`<button class="choice ${st.time===x?'selected':''}" data-k="time" data-v="${x}" ${t.includes(x)?'disabled style="opacity:.4"':''}>${x}${t.includes(x)?' · ocupada':''}</button>`).join('')}</div>`}
   if(s===5)h=`<div class="panel"><p><strong>${svc().name}</strong> con ${sp().name}</p><p class="small">${st.date} · ${st.time} · ${svc().duration} min</p><label class="lbl">Nombre completo</label><input class="field" id="f-name" value="${st.name}"><label class="lbl">Correo</label><input class="field" id="f-email" type="email" value="${st.email}"><label class="lbl">Teléfono</label><input class="field" id="f-phone" value="${st.phone}"><p class="small" style="margin-top:14px">Al reservar se crea tu ficha de clienta para registrar observaciones de la atención.</p></div>`;
   if(s===6)h=`<div class="panel"><p class="eyebrow">Abono obligatorio</p><h3 style="font-size:34px;margin:8px 0">${money(DEPOSIT)} CLP</h3><p class="small">Sin abono no hay reserva. Se descuenta del total (${money(svc().price)}).</p><label class="lbl">Número de tarjeta (simulado)</label><input class="field" placeholder="4242 4242 4242 4242"><div class="grid g2"><div><label class="lbl">Vencimiento</label><input class="field" placeholder="MM/AA"></div><div><label class="lbl">CVV</label><input class="field" placeholder="123"></div></div><label class="row small" style="margin-top:16px"><input type="checkbox" id="f-pay" ${st.paid?'checked':''}> Acepto pagar el abono de ${money(DEPOSIT)} para confirmar mi hora</label></div>`;
   if(s===7)h=`<div class="panel" style="text-align:center"><p class="eyebrow">Código ${st.code}</p><h3 style="font-size:34px;margin:10px 0">Te esperamos, ${st.name.split(' ')[0]}</h3><p class="small">${svc().name} con ${sp().name} · ${st.date} a las ${st.time}<br>Abono pagado: ${money(DEPOSIT)} · Saldo en el salón: ${money(svc().price-DEPOSIT)}</p><a href="index.html" class="btn" style="margin-top:20px">Volver al inicio</a></div>`;
   $('#steps').innerHTML=s<7?[1,2,3,4,5,6].map(n=>`<span class="step ${n===s?'current':''}">${n}</span>`).join(''):'';
   $('#title').textContent=titles[s-1]; $('#content').innerHTML=h;
   $('#nav').classList.toggle('hidden',s===7);
   $('#back').disabled=s===1;
   const ok=[0,st.service,st.specialist,st.date,st.time,1,st.paid][s];
   $('#next').disabled=!ok; $('#next').textContent=s===6?`Pagar ${money(DEPOSIT)} y reservar`:'Continuar';
   $('#summary').innerHTML=`<p class="eyebrow">Tu momento en Nicolett</p><p style="margin-top:12px">${svc()?.name||'—'}</p><p class="small">${sp()?.name||'Especialista por elegir'}<br>${st.date||'Fecha'} · ${st.time||'Hora'}</p><hr style="border:0;border-top:1px solid var(--border);margin:14px 0"><div class="row between"><span class="small">Abono</span><strong>${money(DEPOSIT)}</strong></div>`;
   $('#content').querySelectorAll('[data-k]').forEach(b=>b.onclick=()=>{st[b.dataset.k]=b.dataset.v;if(b.dataset.k==='date')st.time=null;draw()});
   const pay=$('#f-pay'); if(pay)pay.onchange=()=>{st.paid=pay.checked;draw()};
  }
  $('#back').onclick=()=>{st.step--;draw()};
  $('#next').onclick=()=>{
   if(st.step===5){st.name=$('#f-name').value.trim();st.email=$('#f-email').value.trim();st.phone=$('#f-phone').value.trim();
    if(st.name.length<3||!/^\S+@\S+\.\S+$/.test(st.email)||st.phone.length<8){alert('Completa nombre, correo válido y teléfono.');return}}
   if(st.step===6){st.code='NS-'+Math.random().toString(36).slice(2,7).toUpperCase();store.add({id:Date.now()+'',client:st.name,email:st.email,service:svc().name,specialist:st.specialist,date:st.date,time:st.time,status:'Confirmada'})}
   st.step++;draw();
  };
  draw();
 },

 agenda(){
  let monday=new Date('2026-10-05T12:00:00'),filter='todas',sel=null;
  const hours=['09:00','10:00','11:00','12:00','15:00','16:00','17:00','18:00'];
  $('#filter').innerHTML='<option value="todas">Todas las especialistas</option>'+SPECIALISTS.map(s=>`<option value="${s.id}">${s.name}</option>`).join('');
  $('#filter').onchange=e=>{filter=e.target.value;draw()};
  $('#prev').onclick=()=>{monday.setDate(monday.getDate()-7);draw()};
  $('#nextw').onclick=()=>{monday.setDate(monday.getDate()+7);draw()};
  function draw(){
   const days=[...Array(5)].map((_,i)=>{const d=new Date(monday);d.setDate(d.getDate()+i);return d});
   const appts=store.appointments.filter(a=>filter==='todas'||a.specialist===filter);
   $('#week').textContent=`${days[0].toLocaleDateString('es-CL',{day:'numeric',month:'long'})} – ${days[4].toLocaleDateString('es-CL',{day:'numeric',month:'long',year:'numeric'})}`;
   let h='<div class="cell"></div>'+days.map(d=>`<div class="cell"><strong>${d.toLocaleDateString('es-CL',{weekday:'short',day:'numeric'})}</strong></div>`).join('');
   hours.forEach(t=>{h+=`<div class="cell small">${t}</div>`+days.map(d=>{const iso=d.toISOString().slice(0,10);return `<div class="cell">${appts.filter(a=>a.date===iso&&a.time===t).map(a=>`<button class="appt ${a.status==='Anulada'?'off':''}" data-id="${a.id}"><strong>${a.client}</strong><br>${a.service}</button>`).join('')}</div>`}).join('')});
   $('#grid').innerHTML=h;
   $('#grid').querySelectorAll('[data-id]').forEach(b=>b.onclick=()=>open(b.dataset.id));
  }
  function open(id){sel=store.appointments.find(a=>a.id===id);const sp=SPECIALISTS.find(s=>s.id===sel.specialist);
   $('#m-body').innerHTML=`<p class="eyebrow">Detalle de cita</p><h3 style="font-size:28px;margin:8px 0">${sel.client}</h3><p class="small">${sel.service} · ${sp.name}<br>${sel.date} · ${sel.time}</p><p style="margin-top:10px"><span class="status ${sel.status==='Anulada'?'off':''}">${sel.status}</span></p>`;
   $('#m-cancel').classList.toggle('hidden',sel.status==='Anulada');$('#modal').classList.add('open')}
  $('#m-close').onclick=()=>$('#modal').classList.remove('open');
  $('#m-cancel').onclick=()=>{if(confirm('¿Anular esta hora?')){store.cancel(sel.id);$('#modal').classList.remove('open');draw()}};
  draw();
 },

 clientas(){
  let current=null;
  const clients=()=>[...new Set(store.appointments.map(a=>a.client))].sort();
  function list(){const q=$('#q').value.toLowerCase();$('#list').innerHTML=clients().filter(c=>c.toLowerCase().includes(q)).map(c=>`<button class="choice ${c===current?'selected':''}" data-c="${c}" style="margin-bottom:8px"><span class="row"><span class="avatar">${c.split(' ').map(w=>w[0]).join('')}</span>${c}</span></button>`).join('');
   $('#list').querySelectorAll('[data-c]').forEach(b=>b.onclick=()=>{current=b.dataset.c;list();detail()})}
  function detail(){if(!current)return;const visits=store.appointments.filter(a=>a.client===current);const notes=store.notes[current]||[];
   $('#detail').innerHTML=`<p class="eyebrow">Ficha de clienta</p><h2 style="font-size:36px;margin:6px 0 18px">${current}</h2><h3 style="font-size:22px">Historial de visitas</h3>${visits.map(v=>`<div class="row between note" style="background:transparent;border-left-color:var(--border)"><span>${v.date} · ${v.service}<br><span class="small">${SPECIALISTS.find(s=>s.id===v.specialist).name}</span></span><span class="status ${v.status==='Anulada'?'off':''}">${v.status}</span></div>`).join('')}<h3 style="font-size:22px;margin-top:24px">Observaciones de la atención</h3><textarea class="field" id="note" rows="3" style="margin-top:10px" placeholder="Ej: Tinte 7.31 rubio dorado + oxidante 20 vol, 35 min."></textarea><button class="btn sm" id="save" style="margin-top:10px">Guardar observación</button>${notes.map(n=>`<div class="note"><span class="small">${n.date}</span><br>${n.text}</div>`).join('')||'<p class="small" style="margin-top:12px">Sin observaciones aún.</p>'}`;
   $('#save').onclick=()=>{const t=$('#note').value.trim();if(!t)return;const all=store.notes;all[current]=[{text:t,date:new Date().toLocaleString('es-CL')},...(all[current]||[])];store.notes=all;detail()}}
  $('#q').oninput=list; current=clients()[0]; list(); detail();
 },

 dashboard(){
  const a=store.appointments, ok=a.filter(x=>x.status==='Confirmada');
  const price=n=>SERVICES.find(s=>s.name===n)?.price||0;
  $('#kpis').innerHTML=[['Citas confirmadas',ok.length],['Clientas atendidas',new Set(ok.map(x=>x.client)).size],['Abonos recibidos',money(ok.length*DEPOSIT)],['Ingresos estimados',money(ok.reduce((t,x)=>t+price(x.service),0))]].map(([l,v])=>`<div class="panel"><p class="small">${l}</p><p class="kpi">${v}</p></div>`).join('');
  const max=Math.max(1,...SPECIALISTS.map(s=>ok.filter(x=>x.specialist===s.id).length));
  $('#by-sp').innerHTML=SPECIALISTS.map(s=>{const n=ok.filter(x=>x.specialist===s.id).length;return `<div style="margin-top:14px"><div class="row between"><span>${s.name}</span><strong>${n}</strong></div><div class="bar"><i style="width:${n/max*100}%"></i></div></div>`}).join('');
  $('#by-svc').innerHTML=SERVICES.map(s=>{const n=ok.filter(x=>x.service===s.name).length;return `<div class="row between note" style="background:transparent"><span>${s.name}</span><strong>${n}</strong></div>`}).join('');
  $('#rows').innerHTML=a.map(x=>`<tr><td>${x.client}</td><td>${x.service}</td><td>${SPECIALISTS.find(s=>s.id===x.specialist).name}</td><td>${x.date} ${x.time}</td><td><span class="status ${x.status==='Anulada'?'off':''}">${x.status}</span></td></tr>`).join('');
 }
};
document.addEventListener('DOMContentLoaded',()=>{renderShell();pages[document.body.dataset.page]?.()});
