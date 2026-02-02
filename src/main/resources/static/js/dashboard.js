function formatCurrency(amount) {
    return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(amount);
}

$(document).ready(function() {
    const today = new Date();
    const currentMonth = today.getMonth() + 1;
    const currentYear = today.getFullYear();
    
    // Populate Month/Year Selects
    const monthSelect = $('#monthSelect');
    const yearSelect = $('#yearSelect');
    
    const monthNames = ["January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"];
    
    monthNames.forEach((name, index) => {
        monthSelect.append(`<option value="${index + 1}" ${index + 1 === currentMonth ? 'selected' : ''}>${name}</option>`);
    });
    
    for (let i = currentYear; i >= currentYear - 5; i--) {
        yearSelect.append(`<option value="${i}" ${i === currentYear ? 'selected' : ''}>${i}</option>`);
    }

    // Chart instances
    let dailyChart = null;
    let categoryChart = null;

    function loadDashboardData() {
        const month = monthSelect.val();
        const year = yearSelect.val();
        
        // Load Summary
        $.get(`/api/dashboard/summary?month=${month}&year=${year}`, function(data) {
            $('#totalIncome').text(formatCurrency(data.totalIncome));
            $('#totalExpense').text(formatCurrency(data.totalExpense));
            $('#balance').text(formatCurrency(data.balance));
        });

        // Load Daily Chart
        $.get(`/api/dashboard/chart/daily?month=${month}&year=${year}`, function(data) {
            const ctx = document.getElementById('dailyCtx').getContext('2d');
            
            if (dailyChart) dailyChart.destroy();
            
            dailyChart = new Chart(ctx, {
                type: 'bar',
                data: {
                    labels: data.labels,
                    datasets: [{
                        label: 'Expense',
                        data: data.data,
                        backgroundColor: '#4A90E2',
                        borderRadius: 4
                    }]
                },
                options: {
                    maintainAspectRatio: false,
                    plugins: {
                        legend: { display: false }
                    },
                    scales: {
                        y: { beginAtZero: true }
                    }
                }
            });
        });

        // Load Category Chart
        $.get(`/api/dashboard/chart/category?month=${month}&year=${year}`, function(data) {
            const ctx = document.getElementById('categoryCtx').getContext('2d');
            
            if (categoryChart) categoryChart.destroy();
            
            categoryChart = new Chart(ctx, {
                type: 'doughnut',
                data: {
                    labels: data.labels,
                    datasets: [{
                        data: data.data,
                        backgroundColor: data.colors
                    }]
                },
                options: {
                    maintainAspectRatio: false,
                    plugins: {
                        legend: { position: 'bottom' }
                    }
                }
            });
        });
    }

    // Initial Load
    loadDashboardData();

    // Reload on change
    $('#monthSelect, #yearSelect').change(loadDashboardData);
});
