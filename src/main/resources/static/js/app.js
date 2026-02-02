// Common Utilities
const App = {
    formatCurrency: function (amount) {
        return new Intl.NumberFormat('id-VN', { style: 'currency', currency: 'VND' }).format(amount);
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
