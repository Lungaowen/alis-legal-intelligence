document.addEventListener('DOMContentLoaded', () => {
  const messagesContainer = document.getElementById('chatMessages');
  const promptInput = document.getElementById('promptInput');
  const sendBtn = document.getElementById('sendBtn');

  let isLoading = false;

  promptInput.addEventListener('keydown', (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      sendMessage();
    }
  });

  sendBtn.addEventListener('click', sendMessage);

  function sendMessage() {
    const prompt = promptInput.value.trim();
    if (!prompt || isLoading) return;

    addMessage(prompt, 'user');
    promptInput.value = '';

    const loadingMsg = addLoadingMessage();
    isLoading = true;
    sendBtn.disabled = true;

    fetch('/alis/api/chat', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ prompt })
    })
    .then(response => {
      if (!response.ok) {
        return response.json().then(err => { throw new Error(err.error); });
      }
      return response.json();
    })
    .then(data => {
      loadingMsg.remove();
      addMessage(data.response, 'assistant');
    })
    .catch(error => {
      loadingMsg.remove();
      addMessage('❌ Error: ' + error.message, 'error');
    })
    .finally(() => {
      isLoading = false;
      sendBtn.disabled = false;
      promptInput.focus();
    });
  }

  function addMessage(text, sender) {
    const div = document.createElement('div');
    div.className = `message ${sender}`;
    div.textContent = text;
    messagesContainer.appendChild(div);
    messagesContainer.scrollTop = messagesContainer.scrollHeight;
  }

  function addLoadingMessage() {
    const div = document.createElement('div');
    div.className = 'message assistant loading';
    div.innerHTML = '<div class="message-content">Assistant is thinking...</div>';
    messagesContainer.appendChild(div);
    messagesContainer.scrollTop = messagesContainer.scrollHeight;
    return div;
  }

  addMessage("👋 Hello! I'm your senior developer assistant. Ask me anything about coding.", 'assistant');
});
