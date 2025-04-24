// Soochna Update functionality
class SoochnaItem {
    constructor(data) {
        this.id = data.id;
        this.title = data.title;
        this.description = data.description;
        this.status = data.status;
        this.timestamp = data.timestamp || new Date().toISOString();
    }

    getStatusColor() {
        switch (this.status) {
            case 'submitted':
                return 'bg-blue-500';
            case 'pending':
                return 'bg-yellow-500';
            case 'verified':
                return 'bg-green-500';
            case 'rejected':
                return 'bg-red-500';
            default:
                return 'bg-gray-500';
        }
    }

    getStatusText() {
        switch (this.status) {
            case 'submitted':
                return 'जमा किया गया';
            case 'pending':
                return 'समीक्षाधीन';
            case 'verified':
                return 'सत्यापित';
            case 'rejected':
                return 'अस्वीकृत';
            default:
                return 'अज्ञात';
        }
    }

    formatDate(isoString) {
        const date = new Date(isoString);
        return date.toLocaleDateString('hi-IN', {
            year: 'numeric',
            month: 'long',
            day: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
        });
    }

    render() {
        const isRejected = this.status === 'rejected';
        return `
            <div class="bg-white rounded-lg shadow-sm p-4 border ${isRejected ? 'border-red-200' : 'border-gray-100'} hover:shadow-md transition-shadow">
                <div class="flex justify-between items-start">
                    <div class="flex-1">
                        <h3 class="text-lg font-semibold text-gray-800">${this.title}</h3>
                        <p class="text-gray-600 mt-1 text-sm">${this.description}</p>
                        <div class="flex items-center mt-2 text-xs text-gray-500">
                            <i class="fas fa-clock mr-1"></i>
                            ${this.formatDate(this.timestamp)}
                        </div>
                    </div>
                    <div class="ml-4">
                        <span class="text-sm font-medium ${this.getStatusColor()} text-white px-2 py-1 rounded-full">
                            ${this.getStatusText()}
                        </span>
                    </div>
                </div>
                <div class="mt-3">
                    <div class="w-full bg-gray-100 rounded-full h-1.5">
                        ${isRejected ? 
                            `<div class="h-1.5 rounded-full bg-red-500 opacity-50" style="width: 100%"></div>` :
                            `<div class="h-1.5 rounded-full ${this.getStatusColor()} transition-all duration-500" style="width: ${this.getProgressWidth()}%"></div>`
                        }
                    </div>
                    ${isRejected ? 
                        `<p class="text-xs text-red-500 mt-1">यह सूचना अस्वीकृत कर दी गई है</p>` : 
                        ''
                    }
                </div>
            </div>
        `;
    }

    getProgressWidth() {
        switch (this.status) {
            case 'submitted':
                return 25;
            case 'pending':
                return 50;
            case 'verified':
                return 100;
            case 'rejected':
                return 100;
            default:
                return 0;
        }
    }
}

class SoochnaUpdateSection {
    constructor(containerId) {
        this.container = document.getElementById(containerId);
        this.soochnaItems = [];
        this.lastFetchTime = null;
        this.startPolling();
    }

    async fetchSoochnaItems() {
        try {
            let userId = sessionStorage.getItem('userId');
            
            // If userId is not available, try to fetch user details
            if (!userId) {
                try {
                    const response = await fetch(`${config.apiBaseUrl}/api/auth/user`, {
                        headers: {
                            'Authorization': `Bearer ${sessionStorage.getItem('token')}`
                        }
                    });
                    
                    if (response.ok) {
                        const userData = await response.json();
                        userId = userData.id;
                        sessionStorage.setItem('userId', userId);
                    } else {
                        throw new Error('Failed to fetch user details');
                    }
                } catch (error) {
                    console.error('Error fetching user details:', error);
                    this.renderError();
                    return;
                }
            }

            if (!userId) {
                throw new Error('User ID not found');
            }

            const response = await fetch(`${config.apiBaseUrl}/api/occurrences/reporter/${userId}`, {
                headers: {
                    'Authorization': `Bearer ${sessionStorage.getItem('token')}`
                }
            });
            
            if (!response.ok) {
                throw new Error('Failed to fetch soochna items');
            }
            
            const data = await response.json();
            console.log('Received soochna data:', data); // Debug log
            
            const newItems = data.map(item => {
                console.log('Processing item:', item); // Debug log
                return {
                    id: item.id,
                    title: item.title,
                    description: item.description,
                    status: item.status.toLowerCase(),
                    timestamp: item.reportedAt
                };
            }).map(item => new SoochnaItem(item));
            
            // Check for new items
            if (this.lastFetchTime) {
                const newItemsCount = newItems.filter(item => {
                    const timestamp = new Date(item.timestamp);
                    return timestamp > this.lastFetchTime;
                }).length;
                
                if (newItemsCount > 0) {
                    updateSoochnaCount(newItemsCount);
                    this.showNotification(newItemsCount);
                }
            }
            
            console.log('Processed items:', newItems); // Debug log
            this.soochnaItems = newItems;
            this.lastFetchTime = new Date();
            this.render();
        } catch (error) {
            console.error('Error fetching soochna items:', error);
            this.renderError();
        }
    }

    startPolling() {
        // Fetch immediately
        this.fetchSoochnaItems();
        
        // Then fetch every 30 seconds
        setInterval(() => this.fetchSoochnaItems(), 30000);
    }

    showNotification(count) {
        // Only try to show notification if permission is already granted
        if ('Notification' in window && Notification.permission === 'granted') {
            try {
                new Notification('नई सूचना', {
                    body: `आपके पास ${count} नई सूचनाएं हैं`,
                    icon: '/favicon.ico'
                });
            } catch (error) {
                console.log('Notification could not be shown:', error);
            }
        }
        // Don't request permission if it was denied
    }

    render() {
        if (this.soochnaItems.length === 0) {
            this.container.innerHTML = `
                <div class="text-center py-8">
                    <p class="text-gray-500">कोई सूचना नहीं मिली</p>
                </div>
            `;
            return;
        }

        // Sort items but don't filter out rejected ones
        const sortedItems = this.soochnaItems
            .sort((a, b) => new Date(b.timestamp) - new Date(a.timestamp));

        console.log('Rendering items:', sortedItems); // Debug log
        
        this.container.innerHTML = sortedItems
            .map(item => item.render())
            .join('');
    }

    renderError() {
        this.container.innerHTML = `
            <div class="text-center py-8">
                <p class="text-red-500">सूचनाएं लोड करने में त्रुटि। कृपया बाद में पुनः प्रयास करें।</p>
            </div>
        `;
    }
}

// Initialize the section when the page loads
document.addEventListener('DOMContentLoaded', () => {
    const soochnaSection = new SoochnaUpdateSection('soochna-updates');
    
    // Only request notification permission if not already denied
    if ('Notification' in window && Notification.permission === 'default') {
        Notification.requestPermission().catch(error => {
            console.log('Notification permission request failed:', error);
        });
    }
}); 