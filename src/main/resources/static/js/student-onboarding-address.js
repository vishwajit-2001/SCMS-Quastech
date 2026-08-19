(function () {
    const stateDistricts = {
        "Andhra Pradesh": ["Anantapur", "Chittoor", "East Godavari", "Guntur", "Kadapa", "Krishna", "Kurnool", "Nellore", "Prakasam", "Srikakulam", "Tirupati", "Visakhapatnam", "Vizianagaram", "West Godavari", "Other"],
        "Arunachal Pradesh": ["Anjaw", "Changlang", "Dibang Valley", "East Kameng", "East Siang", "Kamle", "Kra Daadi", "Kurung Kumey", "Lepa Rada", "Lohit", "Longding", "Lower Dibang Valley", "Lower Siang", "Lower Subansiri", "Namsai", "Pakke-Kessang", "Papum Pare", "Shi Yomi", "Siang", "Tawang", "Tirap", "Upper Siang", "Upper Subansiri", "West Kameng", "West Siang", "Other"],
        "Assam": ["Bajali", "Baksa", "Barpeta", "Bongaigaon", "Cachar", "Charaideo", "Chirang", "Darrang", "Dhemaji", "Dhubri", "Dibrugarh", "Dima Hasao", "Goalpara", "Golaghat", "Hailakandi", "Hojai", "Jorhat", "Kamrup", "Kamrup Metropolitan", "Karbi Anglong", "Karimganj", "Kokrajhar", "Lakhimpur", "Majuli", "Morigaon", "Nagaon", "Nalbari", "Sivasagar", "Sonitpur", "South Salmara-Mankachar", "Tinsukia", "Udalguri", "West Karbi Anglong", "Other"],
        "Bihar": ["Araria", "Arwal", "Aurangabad", "Banka", "Begusarai", "Bhagalpur", "Bhojpur", "Buxar", "Darbhanga", "East Champaran", "Gaya", "Gopalganj", "Jamui", "Jehanabad", "Kaimur", "Katihar", "Khagaria", "Kishanganj", "Lakhisarai", "Madhepura", "Madhubani", "Munger", "Muzaffarpur", "Nalanda", "Nawada", "Patna", "Purnia", "Rohtas", "Saharsa", "Samastipur", "Saran", "Sheikhpura", "Sheohar", "Sitamarhi", "Siwan", "Supaul", "Vaishali", "West Champaran", "Other"],
        "Chhattisgarh": ["Balod", "Baloda Bazar", "Balrampur", "Bastar", "Bemetara", "Bijapur", "Bilaspur", "Dakshin Bastar Dantewada", "Dhamtari", "Durg", "Gariaband", "Janjgir-Champa", "Jashpur", "Kabirdham", "Kanker", "Kondagaon", "Korba", "Koriya", "Mahasamund", "Mungeli", "Narayanpur", "Raigarh", "Raipur", "Rajnandgaon", "Sukma", "Surajpur", "Surguja", "Other"],
        "Goa": ["North Goa", "South Goa", "Other"],
        "Gujarat": ["Ahmedabad", "Amreli", "Anand", "Aravalli", "Banaskantha", "Bharuch", "Bhavnagar", "Botad", "Chhota Udaipur", "Dahod", "Dang", "Devbhumi Dwarka", "Gandhinagar", "Gir Somnath", "Jamnagar", "Junagadh", "Kheda", "Kutch", "Mahisagar", "Mehsana", "Morbi", "Narmada", "Navsari", "Panchmahal", "Patan", "Porbandar", "Rajkot", "Sabarkantha", "Surat", "Surendranagar", "Tapi", "Vadodara", "Valsad", "Other"],
        "Haryana": ["Ambala", "Bhiwani", "Charkhi Dadri", "Faridabad", "Fatehabad", "Gurugram", "Hisar", "Jhajjar", "Jind", "Kaithal", "Karnal", "Kurukshetra", "Mahendragarh", "Nuh", "Palwal", "Panchkula", "Panipat", "Rewari", "Rohtak", "Sirsa", "Sonipat", "Yamunanagar", "Other"],
        "Himachal Pradesh": ["Bilaspur", "Chamba", "Hamirpur", "Kangra", "Kinnaur", "Kullu", "Lahaul and Spiti", "Mandi", "Shimla", "Sirmaur", "Solan", "Una", "Other"],
        "Jharkhand": ["Bokaro", "Chatra", "Deoghar", "Dhanbad", "Dumka", "East Singhbhum", "Garhwa", "Giridih", "Godda", "Gumla", "Hazaribagh", "Jamtara", "Khunti", "Koderma", "Latehar", "Lohardaga", "Pakur", "Palamu", "Ramgarh", "Ranchi", "Sahebganj", "Seraikela Kharsawan", "Simdega", "West Singhbhum", "Other"],
        "Karnataka": ["Bagalkot", "Ballari", "Belagavi", "Bengaluru Rural", "Bengaluru Urban", "Bidar", "Chamarajanagar", "Chikkaballapur", "Chikkamagaluru", "Chitradurga", "Dakshina Kannada", "Davanagere", "Dharwad", "Gadag", "Hassan", "Haveri", "Kalaburagi", "Kodagu", "Kolar", "Koppal", "Mandya", "Mysuru", "Raichur", "Ramanagara", "Shivamogga", "Tumakuru", "Udupi", "Uttara Kannada", "Vijayapura", "Yadgir", "Other"],
        "Kerala": ["Alappuzha", "Ernakulam", "Idukki", "Kannur", "Kasaragod", "Kollam", "Kottayam", "Kozhikode", "Malappuram", "Palakkad", "Pathanamthitta", "Thiruvananthapuram", "Thrissur", "Wayanad", "Other"],
        "Madhya Pradesh": ["Agar Malwa", "Alirajpur", "Anuppur", "Ashoknagar", "Balaghat", "Barwani", "Betul", "Bhind", "Bhopal", "Burhanpur", "Chhatarpur", "Chhindwara", "Damoh", "Datia", "Dewas", "Dhar", "Dindori", "Guna", "Gwalior", "Harda", "Hoshangabad", "Indore", "Jabalpur", "Jhabua", "Katni", "Khandwa", "Khargone", "Mandla", "Mandsaur", "Morena", "Narsinghpur", "Neemuch", "Panna", "Raisen", "Rajgarh", "Ratlam", "Rewa", "Sagar", "Satna", "Sehore", "Seoni", "Shahdol", "Shajapur", "Sheopur", "Shivpuri", "Sidhi", "Singrauli", "Tikamgarh", "Ujjain", "Vidisha", "Other"],
        "Maharashtra": ["Ahmednagar", "Akola", "Amravati", "Aurangabad", "Beed", "Bhandara", "Buldhana", "Chandrapur", "Dhule", "Gadchiroli", "Gondia", "Hingoli", "Jalgaon", "Jalna", "Kolhapur", "Latur", "Mumbai City", "Mumbai Suburban", "Nagpur", "Nanded", "Nandurbar", "Nashik", "Palghar", "Parbhani", "Pune", "Raigad", "Ratnagiri", "Sangli", "Satara", "Sindhudurg", "Solapur", "Thane", "Wardha", "Washim", "Yavatmal", "Other"],
        "Manipur": ["Bishnupur", "Chandel", "Churachandpur", "Imphal East", "Imphal West", "Jiribam", "Kakching", "Kamjong", "Kangpokpi", "Noney", "Pherzawl", "Senapati", "Tamenglong", "Tengnoupal", "Thoubal", "Ukhrul", "Other"],
        "Meghalaya": ["East Garo Hills", "East Jaintia Hills", "East Khasi Hills", "Mairang", "North Garo Hills", "Ri-Bhoi", "South Garo Hills", "South West Garo Hills", "South West Khasi Hills", "West Garo Hills", "West Jaintia Hills", "West Khasi Hills", "Other"],
        "Mizoram": ["Aizawl", "Champhai", "Hnahthial", "Khawzawl", "Kolasib", "Lawngtlai", "Lunglei", "Mamit", "Saitual", "Serchhip", "Other"],
        "Nagaland": ["Chumoukedima", "Dimapur", "Kiphire", "Kohima", "Longleng", "Mokokchung", "Mon", "Noklak", "Peren", "Phek", "Tuensang", "Wokha", "Zunheboto", "Other"],
        "Odisha": ["Angul", "Balangir", "Balasore", "Bargarh", "Bhadrak", "Boudh", "Cuttack", "Deogarh", "Dhenkanal", "Gajapati", "Ganjam", "Jagatsinghpur", "Jajpur", "Jharsuguda", "Kalahandi", "Kandhamal", "Kendrapara", "Keonjhar", "Khordha", "Koraput", "Malkangiri", "Mayurbhanj", "Nabarangpur", "Nayagarh", "Nuapada", "Puri", "Rayagada", "Sambalpur", "Subarnapur", "Sundargarh", "Other"],
        "Punjab": ["Amritsar", "Barnala", "Bathinda", "Faridkot", "Fatehgarh Sahib", "Fazilka", "Firozpur", "Gurdaspur", "Hoshiarpur", "Jalandhar", "Kapurthala", "Ludhiana", "Mansa", "Moga", "Muktsar", "Pathankot", "Patiala", "Rupnagar", "Sangrur", "SAS Nagar", "Shaheed Bhagat Singh Nagar", "Tarn Taran", "Other"],
        "Rajasthan": ["Ajmer", "Alwar", "Banswara", "Baran", "Barmer", "Bharatpur", "Bhilwara", "Bikaner", "Bundi", "Chittorgarh", "Churu", "Dausa", "Dholpur", "Dungarpur", "Ganganagar", "Hanumangarh", "Jaipur", "Jaisalmer", "Jalore", "Jhalawar", "Jhunjhunu", "Jodhpur", "Karauli", "Kota", "Nagaur", "Pali", "Pratapgarh", "Rajsamand", "Sawai Madhopur", "Sikar", "Sirohi", "Tonk", "Udaipur", "Other"],
        "Sikkim": ["East Sikkim", "North Sikkim", "South Sikkim", "West Sikkim", "Other"],
        "Tamil Nadu": ["Ariyalur", "Chengalpattu", "Chennai", "Coimbatore", "Cuddalore", "Dharmapuri", "Dindigul", "Erode", "Kallakurichi", "Kancheepuram", "Karur", "Krishnagiri", "Madurai", "Mayiladuthurai", "Nagapattinam", "Namakkal", "Nilgiris", "Perambalur", "Pudukkottai", "Ramanathapuram", "Ranipet", "Salem", "Sivaganga", "Tenkasi", "Thanjavur", "Theni", "Thoothukudi", "Tiruchirappalli", "Tirunelveli", "Tirupathur", "Tiruppur", "Tiruvallur", "Tiruvannamalai", "Vellore", "Viluppuram", "Virudhunagar", "Other"],
        "Telangana": ["Adilabad", "Hyderabad", "Jagtial", "Jangaon", "Jayashankar Bhupalpally", "Jogulamba Gadwal", "Kamareddy", "Karimnagar", "Khammam", "Komaram Bheem", "Mahabubabad", "Mahabubnagar", "Mancherial", "Medak", "Medchal-Malkajgiri", "Mulugu", "Nagarkurnool", "Nalgonda", "Narayanpet", "Nirmal", "Nizamabad", "Peddapalli", "Rajanna Sircilla", "Rangareddy", "Sangareddy", "Siddipet", "Suryapet", "Vikarabad", "Warangal Rural", "Warangal Urban", "Yadadri Bhuvanagiri", "Other"],
        "Tripura": ["Dhalai", "Gomati", "Khowai", "North Tripura", "Sepahijala", "South Tripura", "Unakoti", "West Tripura", "Other"],
        "Uttar Pradesh": ["Agra", "Aligarh", "Ambedkar Nagar", "Amethi", "Amroha", "Auraiya", "Ayodhya", "Azamgarh", "Baghpat", "Bahraich", "Ballia", "Banda", "Barabanki", "Bareilly", "Basti", "Bhadohi", "Bijnor", "Budaun", "Bulandshahr", "Chandauli", "Chitrakoot", "Deoria", "Etah", "Etawah", "Faizabad", "Farrukhabad", "Fatehpur", "Firozabad", "Gautam Buddha Nagar", "Ghaziabad", "Ghazipur", "Gonda", "Gorakhpur", "Hamirpur", "Hapur", "Hardoi", "Hathras", "Jalaun", "Jaunpur", "Jhansi", "Kannauj", "Kanpur Nagar", "Kasganj", "Kaushambi", "Kushinagar", "Lakhimpur Kheri", "Lucknow", "Maharajganj", "Mainpuri", "Mathura", "Meerut", "Mirzapur", "Moradabad", "Muzaffarnagar", "Pilibhit", "Prayagraj", "Raebareli", "Rampur", "Saharanpur", "Sambhal", "Sant Kabir Nagar", "Shahjahanpur", "Sitapur", "Sonbhadra", "Sultanpur", "Unnao", "Varanasi", "Other"],
        "Uttarakhand": ["Almora", "Bageshwar", "Chamoli", "Champawat", "Dehradun", "Haridwar", "Nainital", "Pauri Garhwal", "Pithoragarh", "Rudraprayag", "Tehri Garhwal", "Udham Singh Nagar", "Uttarkashi", "Other"],
        "West Bengal": ["Alipurduar", "Bankura", "Birbhum", "Cooch Behar", "Dakshin Dinajpur", "Darjeeling", "Hooghly", "Howrah", "Jalpaiguri", "Jhargram", "Kalimpong", "Kolkata", "Malda", "Murshidabad", "Nadia", "North 24 Parganas", "Paschim Bardhaman", "Paschim Medinipur", "Purba Bardhaman", "Purba Medinipur", "Purulia", "South 24 Parganas", "Uttar Dinajpur", "Other"],
        "Andaman and Nicobar Islands": ["Nicobar", "North and Middle Andaman", "South Andaman", "Other"],
        "Chandigarh": ["Chandigarh", "Other"],
        "Dadra and Nagar Haveli and Daman and Diu": ["Dadra and Nagar Haveli", "Daman", "Diu", "Other"],
        "Delhi": ["Central Delhi", "East Delhi", "New Delhi", "North Delhi", "North East Delhi", "North West Delhi", "Shahdara", "South Delhi", "South East Delhi", "South West Delhi", "West Delhi", "Other"],
        "Jammu and Kashmir": ["Anantnag", "Bandipora", "Baramulla", "Budgam", "Doda", "Ganderbal", "Jammu", "Kathua", "Kishtwar", "Kulgam", "Kupwara", "Poonch", "Pulwama", "Rajouri", "Ramban", "Reasi", "Samba", "Shopian", "Srinagar", "Udhampur", "Other"],
        "Ladakh": ["Kargil", "Leh", "Other"],
        "Lakshadweep": ["Agatti", "Amini", "Andrott", "Kavaratti", "Minicoy", "Other"],
        "Puducherry": ["Karaikal", "Mahe", "Puducherry", "Yanam", "Other"]
    };

    function qs(panel, selector) {
        return panel.querySelector(selector);
    }

    function escapeHtml(value) {
        return String(value || "")
            .replaceAll("&", "&amp;")
            .replaceAll("<", "&lt;")
            .replaceAll(">", "&gt;")
            .replaceAll('"', "&quot;")
            .replaceAll("'", "&#39;");
    }

    function fillDistrictSelect(select, stateName, selectedValue) {
        if (!select) return;
        const districts = stateDistricts[stateName] || [];
        const previous = selectedValue || select.dataset.originalSelectedValue || select.dataset.selectedValue || "";
        select.innerHTML = '<option value="">Please Select District</option>' + districts.map(district => {
            const isSelected = district === previous ? " selected" : "";
            return '<option value="' + escapeHtml(district) + '"' + isSelected + ">" + escapeHtml(district) + "</option>";
        }).join("");
        if (previous && districts.includes(previous)) {
            select.value = previous;
        } else {
            select.value = "";
        }
        select.dataset.selectedValue = select.value;
    }

    function toggleCurrentFields(panel, disabled) {
        [
            qs(panel, '[name="currentHouseNumber"]'),
            qs(panel, '[name="currentAddress"]'),
            qs(panel, '[name="currentCountry"]'),
            qs(panel, '[name="currentState"]'),
            qs(panel, '[name="currentDistrict"]'),
            qs(panel, '[name="currentCity"]'),
            qs(panel, '[name="currentPinCode"]')
        ].forEach(field => {
            if (!field) return;
            field.readOnly = disabled && (field.tagName === "INPUT" || field.tagName === "TEXTAREA");
            field.disabled = disabled && field.tagName === "SELECT";
        });
    }

    function copyCurrentFromPermanent(panel) {
        const permanentFields = {
            houseNumber: qs(panel, '[name="permanentHouseNumber"]'),
            addressLine: qs(panel, '[name="permanentAddress"]'),
            country: qs(panel, '[name="permanentCountry"]'),
            state: qs(panel, '[name="permanentState"]'),
            district: qs(panel, '[name="permanentDistrict"]'),
            city: qs(panel, '[name="permanentCity"]'),
            pinCode: qs(panel, '[name="permanentPinCode"]')
        };
        const currentFields = {
            houseNumber: qs(panel, '[name="currentHouseNumber"]'),
            addressLine: qs(panel, '[name="currentAddress"]'),
            country: qs(panel, '[name="currentCountry"]'),
            state: qs(panel, '[name="currentState"]'),
            district: qs(panel, '[name="currentDistrict"]'),
            city: qs(panel, '[name="currentCity"]'),
            pinCode: qs(panel, '[name="currentPinCode"]')
        };

        if (currentFields.houseNumber) currentFields.houseNumber.value = permanentFields.houseNumber?.value || "";
        if (currentFields.addressLine) currentFields.addressLine.value = permanentFields.addressLine?.value || "";
        if (currentFields.country) currentFields.country.value = permanentFields.country?.value || "";
        if (currentFields.state) currentFields.state.value = permanentFields.state?.value || "";
        if (currentFields.district) {
            fillDistrictSelect(currentFields.district, permanentFields.state?.value || "", permanentFields.district?.value || "");
            currentFields.district.value = permanentFields.district?.value || "";
        }
        if (currentFields.city) currentFields.city.value = permanentFields.city?.value || "";
        if (currentFields.pinCode) currentFields.pinCode.value = permanentFields.pinCode?.value || "";
    }

    function updateCorrespondenceLive(panel) {
        const checkbox = qs(panel, "[data-same-address]");
        if (!checkbox || !checkbox.checked) return;
        copyCurrentFromPermanent(panel);
    }

    function syncDistrictsFromSelect(panel, role) {
        const stateSelect = qs(panel, '[name="' + role + 'State"]');
        const districtSelect = qs(panel, '[name="' + role + 'District"]');
        if (!stateSelect || !districtSelect) return;
        fillDistrictSelect(districtSelect, stateSelect.value, districtSelect.value);
        if (role === "permanent") updateCorrespondenceLive(panel);
    }

    function bindPanel(panel) {
        const permanentCountry = qs(panel, '[name="permanentCountry"]');
        const currentCountry = qs(panel, '[name="currentCountry"]');
        const permanentState = qs(panel, '[name="permanentState"]');
        const currentState = qs(panel, '[name="currentState"]');
        const permanentDistrict = qs(panel, '[name="permanentDistrict"]');
        const currentDistrict = qs(panel, '[name="currentDistrict"]');
        const checkbox = qs(panel, "[data-same-address]");
        const permanentInputs = panel.querySelectorAll('[name^="permanent"]');

        if (permanentState) {
            fillDistrictSelect(permanentDistrict, permanentState.value || permanentState.dataset.selectedValue || "", permanentDistrict?.value || "");
        }
        if (currentState) {
            fillDistrictSelect(currentDistrict, currentState.value || currentState.dataset.selectedValue || "", currentDistrict?.value || "");
        }

        if (permanentCountry) {
            permanentCountry.addEventListener("change", function () {
                if (this.value !== "INDIA") {
                    if (permanentState) permanentState.value = "";
                    if (permanentDistrict) fillDistrictSelect(permanentDistrict, "", "");
                    updateCorrespondenceLive(panel);
                }
            });
        }

        if (currentCountry) {
            currentCountry.addEventListener("change", function () {
                if (this.value !== "INDIA") {
                    if (currentState) currentState.value = "";
                    if (currentDistrict) fillDistrictSelect(currentDistrict, "", "");
                }
            });
        }

        if (permanentState) {
            permanentState.addEventListener("change", function () {
                syncDistrictsFromSelect(panel, "permanent");
            });
            permanentState.addEventListener("input", function () {
                syncDistrictsFromSelect(panel, "permanent");
            });
        }

        if (currentState) {
            currentState.addEventListener("change", function () {
                syncDistrictsFromSelect(panel, "current");
            });
            currentState.addEventListener("input", function () {
                syncDistrictsFromSelect(panel, "current");
            });
        }

        if (permanentDistrict) {
            permanentDistrict.addEventListener("focus", function () {
                fillDistrictSelect(this, permanentState?.value || "", this.value);
            });
            permanentDistrict.addEventListener("click", function () {
                fillDistrictSelect(this, permanentState?.value || "", this.value);
            });
        }

        if (currentDistrict) {
            currentDistrict.addEventListener("focus", function () {
                fillDistrictSelect(this, currentState?.value || "", this.value);
            });
            currentDistrict.addEventListener("click", function () {
                fillDistrictSelect(this, currentState?.value || "", this.value);
            });
        }

        if (checkbox) {
            checkbox.addEventListener("change", function () {
                const checked = this.checked;
                toggleCurrentFields(panel, checked);
                if (checked) {
                    copyCurrentFromPermanent(panel);
                }
            });
        }

        permanentInputs.forEach(input => {
            input.addEventListener("input", function () {
                updateCorrespondenceLive(panel);
            });
            input.addEventListener("change", function () {
                updateCorrespondenceLive(panel);
            });
        });

        const pinInputs = [qs(panel, '[name="permanentPinCode"]'), qs(panel, '[name="currentPinCode"]')];
        pinInputs.forEach(input => {
            if (!input) return;
            input.addEventListener("input", function () {
                this.value = this.value.replace(/[^0-9]/g, "");
            });
        });

        toggleCurrentFields(panel, Boolean(checkbox && checkbox.checked));
        if (checkbox && checkbox.checked) {
            copyCurrentFromPermanent(panel);
        }
    }

    function init() {
        document.querySelectorAll('[data-step-panel="address"]').forEach(bindPanel);
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", init);
    } else {
        init();
    }
    window.addEventListener("load", init);
})();
