document.addEventListener('DOMContentLoaded', () => {
    const loginScreen = document.getElementById('login-screen');
    const appShell = document.getElementById('app-shell');
    const loginForm = document.getElementById('login-form');
    const loginAccountInput = document.getElementById('login-account-input');
    const loginPinInput = document.getElementById('login-pin-input');
    const loginError = document.getElementById('login-error');
    const btnLogin = document.getElementById('btn-login');
    const registerForm = document.getElementById('register-form');
    const registerNameInput = document.getElementById('register-name-input');
    const registerLastNameInput = document.getElementById('register-lastname-input');
    const registerAccountInput = document.getElementById('register-account-input');
    const registerPinInput = document.getElementById('register-pin-input');
    const registerError = document.getElementById('register-error');
    const btnRegister = document.getElementById('btn-register');
    const btnShowLogin = document.getElementById('btn-show-login');
    const btnShowRegister = document.getElementById('btn-show-register');
    const btnLogout = document.getElementById('btn-logout');
    const clientNameDisplay = document.getElementById('client-name-display');
    const messageModal = document.getElementById('message-modal');
    const messageModalTitle = document.getElementById('message-modal-title');
    const messageModalText = document.getElementById('message-modal-text');
    const messageModalClose = document.getElementById('message-modal-close');
    const messageModalIcon = document.getElementById('message-modal-icon');

    const screenTransfer = document.getElementById('screen-transfer');
    const screenConfirm = document.getElementById('screen-confirm');
    const screenSuccess = document.getElementById('screen-success');

    const transferForm = document.getElementById('transfer-form');
    const accountInput = document.getElementById('account-input');
    const amountInput = document.getElementById('amount-input');
    const amountError = document.getElementById('amount-error');
    const displayBalance = document.getElementById('display-balance');

    const confirmAmount = document.getElementById('confirm-amount');
    const confirmName = document.getElementById('confirm-name');
    const confirmAccount = document.getElementById('confirm-account');

    const successAmount = document.getElementById('success-amount');
    const successName = document.getElementById('success-name');
    const successBalance = document.getElementById('success-balance');

    const btnTransferir = document.getElementById('btn-transferir');
    const btnConfirmar = document.getElementById('btn-confirmar');
    const btnBack = document.getElementById('btn-back');
    const btnNewTransfer = document.getElementById('btn-new-transfer');

    const historyList = document.getElementById('history-list');
    const historyAccountBadge = document.getElementById('history-account-badge');

    let activeClientId = null;
    let activeClientBalance = 0.00;
    let activeClientAccount = '';
    let activeClientName = '';
    let selectedRecipientName = '';

    initializeSession();

    btnShowLogin.addEventListener('click', () => setAuthMode('login'));
    btnShowRegister.addEventListener('click', () => setAuthMode('register'));

    [loginAccountInput, registerAccountInput].forEach((input) => {
        input.addEventListener('input', (e) => {
            e.target.value = formatAccountInput(e.target.value);
        });
    });

    async function initializeSession() {
        try {
            const response = await fetch('/api/auth/me');
            const data = await response.json();

            if (data.authenticated && data.client) {
                setLoggedInClient(data.client);
                showApp();
                await loadClientData(activeClientId);
            } else {
                showLogin();
            }
        } catch (error) {
            console.error('Error al verificar sesión:', error);
            showLogin();
        }
    }

    loginForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        loginError.textContent = '';
        setLoading(btnLogin, true);

        try {
            const response = await fetch('/api/auth/login', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    numero_cuenta: loginAccountInput.value.trim(),
                    pin: loginPinInput.value.trim()
                })
            });

            const data = await response.json();

            if (data.success && data.client) {
                setLoggedInClient(data.client);
                showApp();
                resetTransferForm();
                await loadClientData(activeClientId);
            } else {
                loginError.textContent = data.message || 'No se pudo iniciar sesión.';
            }
        } catch (error) {
            console.error('Error al iniciar sesión:', error);
            loginError.textContent = 'Error de conexión con el servidor.';
        } finally {
            setLoading(btnLogin, false);
        }
    });

    registerForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        registerError.textContent = '';
        setLoading(btnRegister, true);

        try {
            const response = await fetch('/api/auth/register', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    nombre: registerNameInput.value.trim(),
                    apellido: registerLastNameInput.value.trim(),
                    numero_cuenta: registerAccountInput.value.trim(),
                    pin: registerPinInput.value.trim()
                })
            });

            const data = await response.json();

            if (data.success && data.client) {
                setLoggedInClient(data.client);
                resetRegisterForm();
                showMessageModal('Cuenta creada', data.message || 'Tu cuenta fue creada correctamente.', 'success');
                showApp();
                await loadClientData(activeClientId);
            } else {
                registerError.textContent = data.message || 'No se pudo crear la cuenta.';
            }
        } catch (error) {
            console.error('Error al crear la cuenta:', error);
            registerError.textContent = 'Error de conexión con el servidor.';
        } finally {
            setLoading(btnRegister, false);
        }
    });

    btnLogout.addEventListener('click', async () => {
        try {
            await fetch('/api/auth/logout', { method: 'POST' });
        } catch (error) {
            console.error('Error al cerrar sesión:', error);
        } finally {
            activeClientId = null;
            activeClientBalance = 0.00;
            activeClientAccount = '';
            activeClientName = '';
            selectedRecipientName = '';
            resetTransferForm();
            historyList.innerHTML = '';
            showLogin();
        }
    });

    messageModalClose.addEventListener('click', hideMessageModal);
    messageModal.addEventListener('click', (event) => {
        if (event.target === messageModal || event.target.classList.contains('message-modal__backdrop')) {
            hideMessageModal();
        }
    });

    async function loadClientData(clientId) {
        try {
            const response = await fetch(`/api/clients/${clientId}`);
            if (response.status === 401 || response.status === 403) {
                showLogin();
                return;
            }

            const data = await response.json();

            if (data.success) {
                activeClientBalance = parseFloat(data.client.saldo);
                activeClientAccount = data.client.numero_cuenta;
                activeClientName = `${data.client.nombre} ${data.client.apellido}`;

                displayBalance.textContent = activeClientBalance.toFixed(2);
                historyAccountBadge.textContent = data.client.numero_cuenta;
                clientNameDisplay.textContent = activeClientName;

                loadTransactions(clientId);
            }
        } catch (error) {
            console.error('Error al obtener datos del cliente:', error);
        }
    }

    async function loadTransactions(clientId) {
        try {
            const response = await fetch(`/api/clients/${clientId}/transactions`);
            if (response.status === 401 || response.status === 403) {
                showLogin();
                return;
            }

            const data = await response.json();

            if (data.success) {
                renderTransactions(data.transactions);
            }
        } catch (error) {
            console.error('Error al obtener transacciones:', error);
        }
    }

    function renderTransactions(transactions) {
        historyList.innerHTML = '';

        if (transactions.length === 0) {
            historyList.innerHTML = `
                <div class="empty-history">
                    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="36" height="36" fill="currentColor">
                        <path d="M13 3c-4.97 0-9 4.03-9 9H1l3.89 3.89.07.14L9 12H6c0-3.87 3.13-7 7-7s7 3.13 7 7-3.13 7-7 7c-1.93 0-3.68-.79-4.94-2.06l-1.42 1.42C8.27 19.99 10.51 21 13 21c4.97 0 9-4.03 9-9s-3.03-9-9-9zm-1 5v5l4.25 2.52.77-1.28-3.52-2.09V8z"/>
                    </svg>
                    <p>No hay transacciones registradas para este cliente.</p>
                </div>
            `;
            return;
        }

        transactions.forEach(tx => {
            const item = document.createElement('div');
            item.className = `transaction-item ${tx.tipo}`;

            const amount = Math.abs(parseFloat(tx.monto_visible ?? tx.monto));
            const sign = tx.tipo === 'ingreso' ? '+ ' : '- ';
            const title = tx.tipo === 'ingreso' ? 'Saldo ingresado' : tx.nombre_destinatario;

            item.innerHTML = `
                <div class="transaction-info">
                    <span class="transaction-name">${title}</span>
                    <span class="transaction-meta">${tx.cuenta_destino} • ${tx.fecha_formateada}</span>
                </div>
                <span class="transaction-amount">${sign}S/ ${amount.toFixed(2)}</span>
            `;

            historyList.appendChild(item);
        });
    }

    amountInput.addEventListener('input', () => {
        validateAmount();
    });

    function validateAmount() {
        const amount = parseFloat(amountInput.value);
        if (isNaN(amount) || amount <= 0) {
            amountError.textContent = 'Monto debe ser mayor a 0.';
            return false;
        } else if (amount > activeClientBalance) {
            amountError.textContent = 'Saldo insuficiente.';
            return false;
        } else {
            amountError.textContent = '';
            return true;
        }
    }

    transferForm.addEventListener('submit', async (e) => {
        e.preventDefault();

        const account = accountInput.value;
        const amount = parseFloat(amountInput.value);

        if (account.length < 13) {
            showMessageModal('Cuenta inválida', 'Ingresa una cuenta válida con el formato 009-XXXXXX-XX.');
            return;
        }

        if (account === activeClientAccount) {
            showMessageModal('Cuenta no permitida', 'No puedes transferirte a tu propia cuenta. Elige otra cuenta destino.');
            return;
        }

        if (!validateAmount()) {
            return;
        }

        setLoading(btnTransferir, true);

        try {
            const response = await fetch(`/api/recipient?cuenta=${encodeURIComponent(account)}`);
            if (response.status === 401 || response.status === 403) {
                showLogin();
                return;
            }

            const data = await response.json();

            if (data.success) {
                selectedRecipientName = data.name;
                confirmAmount.textContent = `S/ ${amount.toFixed(2)}`;
                confirmAccount.textContent = account;
                confirmName.textContent = selectedRecipientName;

                switchScreen(screenTransfer, screenConfirm);
            } else {
                showMessageModal('Usuario no encontrado', data.message || 'No encontramos un usuario registrado con esa cuenta. Verifica el número e inténtalo otra vez.');
            }
        } catch (error) {
            console.error('Error al obtener destinatario:', error);
            showMessageModal('Problema de conexión', 'No pudimos consultar el destinatario en este momento. Intenta de nuevo en unos segundos.');
        } finally {
            setLoading(btnTransferir, false);
        }
    });

    btnConfirmar.addEventListener('click', async () => {
        const account = confirmAccount.textContent;
        const amount = parseFloat(amountInput.value);

        setLoading(btnConfirmar, true);

        try {
            const response = await fetch('/api/transfer', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    cliente_origen_id: activeClientId,
                    cuenta_destino: account,
                    monto: amount,
                    nombre_destinatario: selectedRecipientName
                })
            });

            if (response.status === 401 || response.status === 403) {
                showLogin();
                return;
            }

            const data = await response.json();

            if (data.success) {
                activeClientBalance = data.new_balance;
                displayBalance.textContent = activeClientBalance.toFixed(2);

                successAmount.textContent = `S/ ${amount.toFixed(2)}`;
                successName.textContent = selectedRecipientName;
                successBalance.textContent = `S/ ${activeClientBalance.toFixed(2)}`;

                loadTransactions(activeClientId);
                switchScreen(screenConfirm, screenSuccess);
            } else {
                showMessageModal('No se pudo completar', data.message || 'Ocurrió un error al procesar la transferencia.');
            }
        } catch (error) {
            console.error('Error al realizar la transferencia:', error);
            showMessageModal('Problema de conexión', 'No pudimos completar la transferencia por un problema de conexión. Intenta nuevamente.');
        } finally {
            setLoading(btnConfirmar, false);
        }
    });

    btnBack.addEventListener('click', () => {
        switchScreen(screenConfirm, screenTransfer);
    });

    btnNewTransfer.addEventListener('click', () => {
        resetTransferForm();
        switchScreen(screenSuccess, screenTransfer);
    });

    function switchScreen(fromScreen, toScreen) {
        if (fromScreen.classList.contains('active')) {
            fromScreen.classList.remove('active');
            setTimeout(() => {
                toScreen.classList.add('active');
            }, 150);
        }
    }

    function resetTransferForm() {
        accountInput.value = '';
        amountInput.value = '';
        amountError.textContent = '';
    }

    function setLoggedInClient(client) {
        activeClientId = client.id;
        activeClientBalance = parseFloat(client.saldo);
        activeClientAccount = client.numero_cuenta;
        activeClientName = `${client.nombre} ${client.apellido}`;
    }

    function showApp() {
        loginScreen.classList.add('hidden');
        appShell.classList.remove('hidden');
        clientNameDisplay.textContent = activeClientName || '--';
    }

    function showLogin() {
        appShell.classList.add('hidden');
        loginScreen.classList.remove('hidden');
        loginError.textContent = '';
        registerError.textContent = '';
        loginForm.reset();
        resetRegisterForm();
        setAuthMode('login');
    }

    function setLoading(button, isLoading) {
        const span = button.querySelector('span');
        const spinner = button.querySelector('.spinner');

        if (isLoading) {
            span.classList.add('hidden');
            spinner.classList.remove('hidden');
            button.disabled = true;
        } else {
            span.classList.remove('hidden');
            spinner.classList.add('hidden');
            button.disabled = false;
        }
    }

    function showMessageModal(title, message, type = 'error') {
        messageModalTitle.textContent = title;
        messageModalText.textContent = message;
        messageModalIcon.textContent = type === 'success' ? '✓' : '!';
        messageModalIcon.dataset.type = type;
        messageModal.classList.remove('hidden');
    }

    function hideMessageModal() {
        messageModal.classList.add('hidden');
    }

    function setAuthMode(mode) {
        const isLogin = mode === 'login';
        loginForm.classList.toggle('hidden', !isLogin);
        registerForm.classList.toggle('hidden', isLogin);
        btnShowLogin.classList.toggle('active', isLogin);
        btnShowRegister.classList.toggle('active', !isLogin);
    }

    function resetRegisterForm() {
        registerForm.reset();
        registerError.textContent = '';
    }

    function formatAccountInput(value) {
        let digits = value.replace(/\D/g, '');

        if (digits.length > 11) {
            digits = digits.substring(0, 11);
        }

        let formatted = '';
        if (digits.length > 0) {
            formatted += digits.substring(0, 3);
            if (digits.length > 3) {
                formatted += '-' + digits.substring(3, 9);
                if (digits.length > 9) {
                    formatted += '-' + digits.substring(9, 11);
                }
            }
        }

        return formatted;
    }
});
