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

    // Estado local
    let currentBalance = 80.00;
    let selectedRecipientName = '';

    // Inicialización: Obtener saldo actual
    fetchBalance();

    // 1. Formateo y Máscara de cuenta destino (009-XXXXXX-XX)
    accountInput.addEventListener('input', (e) => {
        let value = e.target.value.replace(/\D/g, ''); // Eliminar todo lo que no sea dígito
        
        // Limitar a un máximo de 11 dígitos reales (3 + 6 + 2)
        if (value.length > 11) {
            value = value.substring(0, 11);
        }

        let formatted = '';
        if (value.length > 0) {
            // Primer bloque (hasta 3 dígitos)
            formatted += value.substring(0, 3);
            if (value.length > 3) {
                // Segundo bloque (hasta 6 dígitos)
                formatted += '-' + value.substring(3, 9);
                if (value.length > 9) {
                    // Tercer bloque (hasta 2 dígitos)
                    formatted += '-' + value.substring(9, 11);
                }
            }
        }
        
        e.target.value = formatted;
    });

    // 2. Validación de saldo en tiempo real
    amountInput.addEventListener('input', () => {
        validateAmount();
    });

    function validateAmount() {
        const amount = parseFloat(amountInput.value);
        if (isNaN(amount) || amount <= 0) {
            amountError.textContent = 'Monto debe ser mayor a 0.';
            return false;
        } else if (amount > currentBalance) {
            amountError.textContent = 'Saldo insuficiente.';
            return false;
        } else {
            amountError.textContent = '';
            return true;
        }
    }

    // 3. Enviar datos al presionar "Transferir" (Paso 1 -> Paso 2)
    transferForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        
        const account = accountInput.value;
        const amount = parseFloat(amountInput.value);

        // Validaciones previas
        if (account.length < 13) {
            alert('Por favor ingrese una cuenta válida en formato 009-XXXXXX-XX');
            return;
        }

        if (!validateAmount()) {
            return;
        }

        // Mostrar cargando en botón
        setLoading(btnTransferir, true);

        try {
            // Consultar el destinatario en el backend
            const response = await fetch(`/api/recipient?cuenta=${encodeURIComponent(account)}`);
            const data = await response.json();

            if (data.success) {
                selectedRecipientName = data.name;

                // Llenar datos de confirmación
                confirmAmount.textContent = `S/ ${amount.toFixed(2)}`;
                confirmAccount.textContent = account;
                confirmName.textContent = selectedRecipientName;

                // Transición a la pantalla de confirmación
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

    // 4. Confirmar transferencia (Paso 2 -> Paso 3)
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
                    cuenta_destino: account,
                    monto: amount,
                    nombre_destinatario: selectedRecipientName
                })
            });

            const data = await response.json();

            if (data.success) {
                // Actualizar saldo local
                currentBalance = data.new_balance;
                displayBalance.textContent = currentBalance.toFixed(2);

                // Llenar pantalla de éxito
                successAmount.textContent = `S/ ${amount.toFixed(2)}`;
                successName.textContent = selectedRecipientName;
                successBalance.textContent = `S/ ${currentBalance.toFixed(2)}`;

                // Transición a pantalla de éxito
                switchScreen(screenConfirm, screenSuccess);
            } else {
                alert(data.message || 'Ocurrió un error al procesar la transferencia.');
            }
        } catch (error) {
            console.error('Error al realizar transferencia:', error);
            alert('Error de conexión con el servidor.');
        } finally {
            setLoading(btnConfirmar, false);
        }
    });

    // 5. Botón volver (Paso 2 -> Paso 1)
    btnBack.addEventListener('click', () => {
        switchScreen(screenConfirm, screenTransfer);
    });

    // 6. Botón realizar otra transferencia (Paso 3 -> Paso 1)
    btnNewTransfer.addEventListener('click', () => {
        // Limpiar inputs
        accountInput.value = '';
        amountInput.value = '';
        amountError.textContent = '';
        
        switchScreen(screenSuccess, screenTransfer);
    });

    // --- Helper Functions ---

    function switchScreen(fromScreen, toScreen) {
        fromScreen.classList.remove('active');
        // Esperamos un instante corto para dar fluidez
        setTimeout(() => {
            toScreen.classList.add('active');
        }, 150);
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

    async function fetchBalance() {
        try {
            const response = await fetch('/api/balance');
            const data = await response.json();
            if (data.success) {
                currentBalance = data.balance;
                displayBalance.textContent = currentBalance.toFixed(2);
            }
        } catch (error) {
            console.error('Error al obtener balance:', error);
        }
    }
});
