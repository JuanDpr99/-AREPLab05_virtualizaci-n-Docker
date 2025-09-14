/* 
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Other/javascript.js to edit this template
 */
document.getElementById('btn-time').onclick = async () => {
  const r = await fetch('/api/time');
  document.getElementById('out-time').textContent = await r.text();
};

document.getElementById('btn-echo').onclick = async () => {
  const msg = encodeURIComponent(document.getElementById('msg').value);
  const r = await fetch('/api/echo?msg=' + msg);
  document.getElementById('out-echo').textContent = await r.text();
};


