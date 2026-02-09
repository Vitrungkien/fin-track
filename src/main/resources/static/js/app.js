// Common Utilities
const App = {
    formatCurrency: function (amount) {
        return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(amount);
    },

    formatDate: function (dateStr) {
        if (!dateStr) return '';
        const date = new Date(dateStr);
        return date.toLocaleDateString('vi-VN');
    },

    showToast: function (message, type = 'success') {
        // Create toast container if not exists
        if ($('.toast-container').length === 0) {
            $('body').append('<div class="toast-container position-fixed bottom-0 end-0 p-3"></div>');
        }

        const toastHtml = `
            <div class="toast align-items-center text-white bg-${type} border-0" role="alert" aria-live="assertive" aria-atomic="true">
                <div class="d-flex">
                    <div class="toast-body">
                        ${message}
                    </div>
                    <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast" aria-label="Close"></button>
                </div>
            </div>
        `;

        const $toast = $(toastHtml);
        $('.toast-container').append($toast);
        const toast = new bootstrap.Toast($toast[0]);
        toast.show();

        $toast.on('hidden.bs.toast', function () {
            $(this).remove();
        });
    }
};

$(document).ajaxSetup({
    beforeSend: function (xhr) {
        // CSRF handling if enabled, but disabled for this PoC
    },
    error: function (xhr) {
        if (xhr.status === 401) {
            window.location.href = '/login';
        }
    }
});

function submitChangePassword() {
    const oldPassword = $('#oldPassword').val();
    const newPassword = $('#newPassword').val();
    const confirmPassword = $('#confirmPassword').val();

    if (newPassword !== confirmPassword) {
        App.showToast('New passwords do not match!', 'danger');
        return;
    }

    if (newPassword.length < 6) {
        App.showToast('Password must be at least 6 characters.', 'danger');
        return;
    }

    const token = localStorage.getItem('token'); // Assuming you might have one if using pure JWT, but here we likely rely on Session or cookie. 
    // However, the requested upgraded auth system uses JWT.
    // If we are in Hybrid mode (Session + JWT), we need to ensure the request is authenticated.
    // Since we kept formLogin, the session cookie is likely present.
    // But let's check if we have to send the Bearer token.
    // Ideally, for a "Change Password" API call, we should attach the JWT if we have it stored.

    // Let's assume standard session-based auth for this page interaction first since it's an MVC app.
    // Actually, the AuthController endpoint is /api/auth/change-password.
    // Wait, the AuthController changePassword uses SecurityContext to get email.
    // If the user logged in via Form, the JSESSIONID is set.
    // However, if the user logged in via JWT (e.g. Mobile App), they use the token.
    // Since this is the Web UI, let's assume valid session or we can try to send the token if found.

    const headers = {
        'Content-Type': 'application/json'
    };

    // If we stored the token in localStorage during login (if we implemented a custom login page that does that)
    // But standard Spring Security Form Login usually doesn't return a JSON with token to the browser unless customized.
    // The current layout uses `th:action="@{/login}"` in SecurityConfig.
    // So likely this is a session-based context. The AuthController logic `SecurityContextHolder.getContext().getAuthentication().getName()` works for both Session and JWT.

    $.ajax({
        url: '/api/auth/change-password',
        type: 'POST',
        headers: headers,
        data: JSON.stringify({
            oldPassword: oldPassword,
            newPassword: newPassword
        }),
        success: function (response) {
            App.showToast('Password changed successfully!', 'success');
            $('#changePasswordModal').modal('hide');
            $('#changePasswordForm')[0].reset();
        },
        error: function (xhr) {
            let errorMsg = 'Failed to change password';
            if (xhr.status === 401) {
                // Redirect to login if unauthorized
                window.location.href = '/login';
                return;
            }
            if (xhr.responseJSON && xhr.responseJSON.message) {
                errorMsg = xhr.responseJSON.message;
            } else if (xhr.responseText) {
                // Try to parse just in case
                try {
                    const json = JSON.parse(xhr.responseText);
                    if (json.message) errorMsg = json.message;
                } catch (e) {
                    errorMsg = xhr.responseText;
                }
            }
            App.showToast(errorMsg, 'danger');
        }
    });
}
