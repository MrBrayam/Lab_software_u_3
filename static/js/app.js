document.addEventListener('DOMContentLoaded', () => {
    // DOM Elements
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

    const clientSelect = document.getElementById('client-select');
    const historyList = document.getElementById('history-list');
    const historyAccountBadge = document.getElementById('history-account-badge');

    // Estado local
    let activeClientId = null;
    let activeClientBalance = 0.00;
    let selectedRecipientName = '';

    // Inicialización: Cargar la lista de clientes
    fetchClients();

    // 1. Cargar clientes en el Selector
    async function fetchClients() {
        try {
            const response = await fetch('/api/clients');
            const data = await response.json();
            
            if (data.success && data.clients.length > 0) {
                clientSelect.innerHTML = '';
                data.clients.forEach(client => {
                    const option = document.createElement('option');
                    option.value = client.id;
                    option.textContent = `${client.nombre} ${client.apellido}`;
                    clientSelect.appendChild(option);
                });

                // Seleccionar por defecto el primer cliente
                activeClientId = data.clients[0].id;
                loadClientData(activeClientId);
            }
        } catch (error) {
            console.error('Error al cargar clientes:', error);
        }
    }

    // Escuchar cambios de selección de cliente
    clientSelect.addEventListener('change', (e) => {
        activeClientId = parseInt(e.target.value);
        loadClientData(activeClientId);
        
        // Resetear formulario si cambian de cliente en medio del proceso
        resetTransferForm();
        switchScreen(screenConfirm, screenTransfer);
        switchScreen(screenSuccess, screenTransfer);
    });

    // 2. Cargar datos del cliente activo (saldo, número de cuenta e historial)
    async function loadClientData(clientId) {
        try {
            const response = await fetch(`/api/clients/${clientId}`);
            const data = await response.json();
            
            if (data.success) {
                activeClientBalance = parseFloat(data.client.saldo);
                displayBalance.textContent = activeClientBalance.toFixed(2);
                historyAccountBadge.textContent = data.client.numero_cuenta;
                
                // Cargar el historial de transacciones
                loadTransactions(clientId);
            }
        } catch (error) {
            console.error('Error al obtener datos del cliente:', error);
        }
    }

    // 3. Cargar Historial de Transacciones desde la BD
    async function loadTransactions(clientId) {
        try {
            const response = await fetch(`/api/clients/${clientId}/transactions`);
            const data = await response.json();
            
            if (data.success) {
                renderTransactions(data.transactions);
            }
        } catch (error) {
            console.error('Error al obtener transacciones:', error);
        }
    }

    // Renderizar transacciones en la interfaz
    function renderTransactions(transactions) {
        historyList.innerHTML = '';

        if (transactions.length === 0) {
            historyList.innerHTML = `
                <div class="empty-history">
                    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="36" height="36" fill="currentColor">
                        <path d="M13 3c-4.97 0-9 4.03-9 9H1l3.89 3.89.07.14L9 12H6c0-3.87 3.13-7 7-7s7 3.13 7 7-3.13 7-7 7c-1.93 0-3.68-.79-4.94-2.06l-1.42 1.42C8.27 19.99 10.51 21 13 21c4.97 0 9-4.03 9-9s-4.03-9-9-9zm-1 5v5l4.25 2.52.77-1.28-3.52-2.09V8z"/>
                    </svg>
                    <p>No hay transacciones registradas para este cliente.</p>
                </div>
            `;
            return;
        }

        transactions.forEach(tx => {
            const item = document.createElement('div');
            item.className = 'transaction-item';
            
            item.innerHTML = `
                <div class="transaction-info">
                    <span class="transaction-name">${tx.nombre_destinatario}</span>
                    <span class="transaction-meta">${tx.cuenta_destino} • ${tx.fecha_formateada}</span>
                </div>
                <span class="transaction-amount">- S/ ${parseFloat(tx.monto).toFixed(2)}</span>
            `;
            
            historyList.appendChild(item);
        });
    }

    // 4. Máscara de cuenta destino (009-XXXXXX-XX)
    accountInput.addEventListener('input', (e) => {
        let value = e.target.value.replace(/\D/g, ''); // Eliminar todo lo que no sea dígito
        
        if (value.length > 11) {
            value = value.substring(0, 11);
        }

        let formatted = '';
        if (value.length > 0) {
            formatted += value.substring(0, 3);
            if (value.length > 3) {
                formatted += '-' + value.substring(3, 9);
                if (value.length > 9) {
                    formatted += '-' + value.substring(9, 11);
                }
            }
        }
        
        e.target.value = formatted;
    });

    // 5. Validación de saldo en tiempo real
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

    // 6. Validar datos y consultar destinatario al presionar "Transferir" (Paso 1 -> Paso 2)
    transferForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        
        const account = accountInput.value;
        const amount = parseFloat(amountInput.value);

        if (account.length < 13) {
            alert('Por favor ingrese una cuenta válida en formato 009-XXXXXX-XX');
            return;
        }

        // Evitar transferirse a sí mismo
        if (account === historyAccountBadge.textContent) {
            alert('No puede transferirse a su propia cuenta. Elija otra cuenta destino.');
            return;
        }

        if (!validateAmount()) {
            return;
        }

        setLoading(btnTransferir, true);

        try {
            const response = await fetch(`/api/recipient?cuenta=${encodeURIComponent(account)}`);
            const data = await response.json();

            if (data.success) {
                selectedRecipientName = data.name;

                // Llenar datos de confirmación
                confirmAmount.textContent = `S/ ${amount.toFixed(2)}`;
                confirmAccount.textContent = account;
                confirmName.textContent = selectedRecipientName;

                switchScreen(screenTransfer, screenConfirm);
            } else {
                alert('No se pudo encontrar el destinatario. Intente nuevamente.');
            }
        } catch (error) {
            console.error('Error al obtener destinatario:', error);
            alert('Error de conexión con el servidor.');
        } finally {
            setLoading(btnTransferir, false);
        }
    });

    // 7. Confirmar transferencia y guardar en la BD en tiempo real (Paso 2 -> Paso 3)
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

            const data = await response.json();

            if (data.success) {
                // Actualizar saldo del cliente activo en pantalla
                activeClientBalance = data.new_balance;
                displayBalance.textContent = activeClientBalance.toFixed(2);

                // Llenar pantalla de éxito
                successAmount.textContent = `S/ ${amount.toFixed(2)}`;
                successName.textContent = selectedRecipientName;
                successBalance.textContent = `S/ ${activeClientBalance.toFixed(2)}`;

                // Recargar el historial de transacciones para incluir el nuevo registro
                loadTransactions(activeClientId);

                // Transición a la pantalla de éxito
                switchScreen(screenConfirm, screenSuccess);
            } else {
                alert(data.message || 'Ocurrió un error al procesar la transferencia.');
            }
        } catch (error) {
            console.error('Error al realizar la transferencia:', error);
            alert('Error de conexión con el servidor.');
        } finally {
            setLoading(btnConfirmar, false);
        }
    });

    // 8. Botón volver (Paso 2 -> Paso 1)
    btnBack.addEventListener('click', () => {
        switchScreen(screenConfirm, screenTransfer);
    });

    // 9. Botón realizar otra transferencia (Paso 3 -> Paso 1)
    btnNewTransfer.addEventListener('click', () => {
        resetTransferForm();
        switchScreen(screenSuccess, screenTransfer);
    });

    // --- Funciones de Utilidad ---

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
});
