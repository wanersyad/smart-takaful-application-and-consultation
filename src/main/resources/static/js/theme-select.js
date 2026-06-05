(function () {
    const SELECTOR = 'select:not([multiple]):not([data-native-select]):not([data-theme-select-ready])';
    const openSelects = new Set();
    let nextId = 0;

    function getOptions(select) {
        return Array.from(select.options).filter(function (option) {
            return !option.hidden;
        });
    }

    function selectedOption(select) {
        return select.options[select.selectedIndex] || getOptions(select)[0];
    }

    function close(wrapper) {
        const button = wrapper.querySelector('.theme-select-button');
        const list = wrapper.querySelector('.theme-select-list');
        if (!button || !list) return;
        wrapper.classList.remove('is-open');
        button.setAttribute('aria-expanded', 'false');
        list.hidden = true;
        openSelects.delete(wrapper);
    }

    function closeOthers(current) {
        openSelects.forEach(function (wrapper) {
            if (wrapper !== current) close(wrapper);
        });
    }

    function setActive(wrapper, index) {
        const options = Array.from(wrapper.querySelectorAll('.theme-select-option:not(:disabled)'));
        if (!options.length) return;
        const next = Math.max(0, Math.min(index, options.length - 1));
        options.forEach(function (option, optionIndex) {
            option.classList.toggle('is-active', optionIndex === next);
            option.setAttribute('aria-selected', optionIndex === next ? 'true' : 'false');
        });
        options[next].scrollIntoView({ block: 'nearest' });
    }

    function updateLabel(wrapper) {
        const select = wrapper.querySelector('select');
        const label = wrapper.querySelector('.theme-select-label');
        const option = selectedOption(select);
        if (!select || !label || !option) return;

        label.textContent = option.textContent.trim();
        Array.from(wrapper.querySelectorAll('.theme-select-option')).forEach(function (button) {
            const selected = button.dataset.value === option.value;
            button.classList.toggle('is-selected', selected);
            button.setAttribute('aria-current', selected ? 'true' : 'false');
        });

        const optionIndex = getOptions(select).findIndex(function (candidate) {
            return candidate.value === option.value;
        });
        setActive(wrapper, Math.max(0, optionIndex));
    }

    function choose(wrapper, optionButton) {
        const select = wrapper.querySelector('select');
        if (!select || !optionButton || optionButton.disabled) return;
        select.value = optionButton.dataset.value;
        updateLabel(wrapper);
        select.dispatchEvent(new Event('change', { bubbles: true }));
        close(wrapper);
        wrapper.querySelector('.theme-select-button').focus();
    }

    function open(wrapper) {
        const select = wrapper.querySelector('select');
        const button = wrapper.querySelector('.theme-select-button');
        const list = wrapper.querySelector('.theme-select-list');
        if (!select || !button || !list || select.disabled) return;
        closeOthers(wrapper);
        wrapper.classList.add('is-open');
        button.setAttribute('aria-expanded', 'true');
        list.hidden = false;
        openSelects.add(wrapper);
        updateLabel(wrapper);
    }

    function enhance(select) {
        if (!select.parentNode) return;

        const wrapper = document.createElement('div');
        const id = 'theme-select-' + (++nextId);
        wrapper.className = 'theme-select';

        const button = document.createElement('button');
        button.type = 'button';
        button.className = 'theme-select-button';
        button.setAttribute('aria-haspopup', 'listbox');
        button.setAttribute('aria-expanded', 'false');
        button.setAttribute('aria-controls', id);
        button.innerHTML = '<span class="theme-select-label"></span><span class="theme-select-chevron" aria-hidden="true"></span>';

        const list = document.createElement('div');
        list.className = 'theme-select-list';
        list.id = id;
        list.role = 'listbox';
        list.hidden = true;

        getOptions(select).forEach(function (option) {
            const item = document.createElement('button');
            item.type = 'button';
            item.className = 'theme-select-option';
            item.role = 'option';
            item.dataset.value = option.value;
            item.textContent = option.textContent.trim();
            if (option.disabled) item.disabled = true;
            item.addEventListener('click', function () {
                choose(wrapper, item);
            });
            list.appendChild(item);
        });

        select.dataset.themeSelectReady = 'true';
        select.classList.add('theme-select-native');
        select.parentNode.insertBefore(wrapper, select);
        wrapper.appendChild(select);
        wrapper.insertBefore(button, select);
        wrapper.appendChild(list);

        if (select.className.indexOf('mt-2') !== -1) {
            wrapper.style.marginTop = '0.5rem';
        }
        if (select.className.indexOf('rounded-full') !== -1) wrapper.classList.add('theme-select-pill');
        if (select.className.indexOf('rounded-md') !== -1 || select.className.indexOf('rounded-lg') !== -1) {
            wrapper.classList.add('theme-select-compact');
        }

        button.addEventListener('click', function () {
            wrapper.classList.contains('is-open') ? close(wrapper) : open(wrapper);
        });

        button.addEventListener('keydown', function (event) {
            const options = Array.from(wrapper.querySelectorAll('.theme-select-option:not(:disabled)'));
            const activeIndex = options.findIndex(function (option) {
                return option.classList.contains('is-active');
            });
            if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
                event.preventDefault();
                open(wrapper);
                setActive(wrapper, event.key === 'ArrowDown' ? activeIndex + 1 : activeIndex - 1);
            } else if (event.key === 'Enter' || event.key === ' ') {
                event.preventDefault();
                if (!wrapper.classList.contains('is-open')) {
                    open(wrapper);
                    return;
                }
                choose(wrapper, options[Math.max(0, activeIndex)]);
            } else if (event.key === 'Escape') {
                close(wrapper);
            }
        });

        select.addEventListener('change', function () {
            updateLabel(wrapper);
        });
        select.addEventListener('theme-select:sync', function () {
            updateLabel(wrapper);
        });

        updateLabel(wrapper);
    }

    document.addEventListener('click', function (event) {
        openSelects.forEach(function (wrapper) {
            if (!wrapper.contains(event.target)) close(wrapper);
        });
    });

    document.addEventListener('DOMContentLoaded', function () {
        document.querySelectorAll(SELECTOR).forEach(enhance);
    });
})();
