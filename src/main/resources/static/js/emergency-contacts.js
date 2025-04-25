// Configuration
const config = {
    apiBaseUrl: ''
};

// Get user info and initialize
async function initializeApp() {
    try {
        // Get user info
        const response = await fetch(`${config.apiBaseUrl}/api/auth/user`, {
            headers: {
                'Authorization': `Bearer ${sessionStorage.getItem('token')}`
            }
        });
        
        if (!response.ok) {
            throw new Error('Failed to get user info');
        }
        
        const userData = await response.json();
        sessionStorage.setItem('userDistrict', userData.district);
        sessionStorage.setItem('userRole', userData.role);
        
        // Initialize district filter
        const districtFilter = document.getElementById('districtFilter');
        if (userData.role !== 'SUPERADMIN') {
            // Non-superadmin users can only see their district
            districtFilter.innerHTML = `<option value="${userData.district}">${userData.district}</option>`;
            districtFilter.disabled = true;
        } else {
            // Superadmin can see all districts
            const districts = ['Tehri Garhwal', 'Dehradun', 'Haridwar', 'Uttarkashi', 'Rudraprayag', 'Chamoli'];
            districtFilter.innerHTML = '<option value="">All Districts</option>' +
                districts.map(d => `<option value="${d}">${d}</option>`).join('');
        }
        
        // Load initial contacts
        await loadContacts();
        
        // Add event listeners for filters
        document.getElementById('districtFilter').addEventListener('change', loadContacts);
        document.getElementById('departmentFilter').addEventListener('change', loadContacts);
        document.getElementById('statusFilter').addEventListener('change', loadContacts);
        
    } catch (error) {
        console.error('Error initializing app:', error);
        window.location.href = 'login.html';
    }
}

// Load contacts based on filters
async function loadContacts() {
    try {
        const district = document.getElementById('districtFilter').value;
        const department = document.getElementById('departmentFilter').value;
        const status = document.getElementById('statusFilter').value;
        
        let url = `${config.apiBaseUrl}/api/emergency-contacts?`;
        if (district) url += `district=${encodeURIComponent(district)}&`;
        if (department) url += `department=${encodeURIComponent(department)}&`;
        if (status !== '') url += `active=${status}`;
        
        const response = await fetch(url, {
            headers: {
                'Authorization': `Bearer ${sessionStorage.getItem('token')}`
            }
        });
        
        if (!response.ok) {
            throw new Error('Failed to load contacts');
        }
        
        const contacts = await response.json();
        displayContacts(contacts);
        
    } catch (error) {
        console.error('Error loading contacts:', error);
        showError('Failed to load contacts');
    }
}

// Display contacts in the UI
function displayContacts(contacts) {
    const contactsList = document.getElementById('contactsList');
    contactsList.innerHTML = '';
    
    if (contacts.length === 0) {
        contactsList.innerHTML = `
            <div class="col-span-full text-center py-8 text-gray-500">
                No emergency contacts found
            </div>
        `;
        return;
    }
    
    contacts.forEach(contact => {
        const card = document.createElement('div');
        card.className = 'contact-card bg-white rounded-lg shadow p-6 hover:shadow-lg transition-shadow';
        card.innerHTML = `
            <div class="flex justify-between items-start mb-4">
                <div>
                    <h3 class="text-lg font-semibold text-gray-900">${contact.name}</h3>
                    <p class="text-sm text-gray-600">${contact.designation}</p>
                </div>
                <span class="px-2 py-1 text-xs font-semibold rounded-full ${
                    contact.isActive ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'
                }">
                    ${contact.isActive ? 'Active' : 'Inactive'}
                </span>
            </div>
            <div class="space-y-2">
                <p class="text-sm">
                    <i class="fas fa-phone-alt text-blue-600 mr-2"></i>
                    <a href="tel:${contact.phoneNumber}" class="text-blue-600 hover:text-blue-800">
                        ${contact.phoneNumber}
                    </a>
                </p>
                <p class="text-sm">
                    <i class="fas fa-building text-gray-600 mr-2"></i>
                    ${contact.department}
                </p>
                <p class="text-sm">
                    <i class="fas fa-map-marker-alt text-gray-600 mr-2"></i>
                    ${contact.district}${contact.area ? ` - ${contact.area}` : ''}
                </p>
            </div>
            ${canManageContact(contact) ? `
                <div class="mt-4 flex justify-end space-x-2">
                    <button onclick="editContact('${contact.id}')" class="text-blue-600 hover:text-blue-800">
                        <i class="fas fa-edit"></i>
                    </button>
                    <button onclick="toggleContactStatus('${contact.id}')" class="text-yellow-600 hover:text-yellow-800">
                        <i class="fas fa-power-off"></i>
                    </button>
                    <button onclick="deleteContact('${contact.id}')" class="text-red-600 hover:text-red-800">
                        <i class="fas fa-trash"></i>
                    </button>
                </div>
            ` : ''}
        `;
        contactsList.appendChild(card);
    });
}

// Check if user can manage (edit/delete) a contact
function canManageContact(contact) {
    const userRole = sessionStorage.getItem('userRole');
    const userDistrict = sessionStorage.getItem('userDistrict');
    
    return userRole === 'SUPERADMIN' || 
           (userRole === 'ADMIN' && userDistrict === contact.district);
}

// Add/Edit contact modal functions
function showAddContactModal() {
    document.getElementById('modalTitle').textContent = 'Add Emergency Contact';
    document.getElementById('contactId').value = '';
    document.getElementById('contactForm').reset();
    document.getElementById('contactModal').classList.add('show');
}

function closeContactModal() {
    document.getElementById('contactModal').classList.remove('show');
}

// Handle form submission
async function handleContactSubmit(event) {
    event.preventDefault();
    
    const contactId = document.getElementById('contactId').value;
    const contact = {
        name: document.getElementById('contactName').value,
        designation: document.getElementById('contactDesignation').value,
        phoneNumber: document.getElementById('contactPhone').value,
        department: document.getElementById('contactDepartment').value,
        district: document.getElementById('contactDistrict').value,
        area: document.getElementById('contactArea').value,
    };
    
    try {
        const url = contactId ? 
            `${config.apiBaseUrl}/api/emergency-contacts/${contactId}` :
            `${config.apiBaseUrl}/api/emergency-contacts`;
            
        const response = await fetch(url, {
            method: contactId ? 'PUT' : 'POST',
            headers: {
                'Authorization': `Bearer ${sessionStorage.getItem('token')}`,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(contact)
        });
        
        if (!response.ok) {
            throw new Error('Failed to save contact');
        }
        
        closeContactModal();
        await loadContacts();
        
    } catch (error) {
        console.error('Error saving contact:', error);
        showError('Failed to save contact');
    }
}

// Delete contact
async function deleteContact(id) {
    if (!confirm('Are you sure you want to delete this contact?')) {
        return;
    }
    
    try {
        const response = await fetch(`${config.apiBaseUrl}/api/emergency-contacts/${id}`, {
            method: 'DELETE',
            headers: {
                'Authorization': `Bearer ${sessionStorage.getItem('token')}`
            }
        });
        
        if (!response.ok) {
            throw new Error('Failed to delete contact');
        }
        
        await loadContacts();
        
    } catch (error) {
        console.error('Error deleting contact:', error);
        showError('Failed to delete contact');
    }
}

// Toggle contact status
async function toggleContactStatus(id) {
    try {
        const response = await fetch(`${config.apiBaseUrl}/api/emergency-contacts/${id}/toggle`, {
            method: 'PUT',
            headers: {
                'Authorization': `Bearer ${sessionStorage.getItem('token')}`
            }
        });
        
        if (!response.ok) {
            throw new Error('Failed to toggle contact status');
        }
        
        await loadContacts();
        
    } catch (error) {
        console.error('Error toggling contact status:', error);
        showError('Failed to toggle contact status');
    }
}

// Show error message
function showError(message) {
    // Implement error notification
    alert(message);
}

// Initialize the app when the page loads
document.addEventListener('DOMContentLoaded', initializeApp);

// Add form submit handler
document.getElementById('contactForm').addEventListener('submit', handleContactSubmit);

// Add logout handler
document.getElementById('logoutBtn').addEventListener('click', () => {
    sessionStorage.clear();
    window.location.href = 'login.html';
}); 