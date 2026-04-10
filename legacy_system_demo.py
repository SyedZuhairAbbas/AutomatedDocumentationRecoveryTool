import datetime
import math

class InventoryManager:
    def __init__(self, store_name, max_capacity):
        self.store_name = store_name
        self.max_capacity = max_capacity
        self.items = {}
        self.transaction_log = []

    def add_item(self, item_id, name, quantity, price):
        if item_id in self.items:
            self.items[item_id]['quantity'] += quantity
        else:
            self.items[item_id] = {'name': name, 'quantity': quantity, 'price': price}
        self.transaction_log.append((datetime.datetime.now(), 'ADD', item_id, quantity))

    def remove_item(self, item_id, quantity):
        if item_id not in self.items:
            return False
        if self.items[item_id]['quantity'] < quantity:
            return False
        self.items[item_id]['quantity'] -= quantity
        self.transaction_log.append((datetime.datetime.now(), 'REMOVE', item_id, quantity))
        return True

    def get_stock_value(self):
        total = 0
        for item_id in self.items:
            total += self.items[item_id]['quantity'] * self.items[item_id]['price']
        return round(total, 2)

    def check_low_stock(self, threshold):
        low = []
        for item_id in self.items:
            if self.items[item_id]['quantity'] < threshold:
                low.append(self.items[item_id]['name'])
        return low


class UserAuthenticator:
    def __init__(self, db_connection):
        self.db = db_connection
        self.active_sessions = {}
        self.failed_attempts = {}

    def login(self, username, password, ip_address):
        if ip_address in self.failed_attempts:
            if self.failed_attempts[ip_address] >= 5:
                return None
        user = self.db.query(username, password)
        if user:
            token = str(hash(username + str(datetime.datetime.now())))
            self.active_sessions[token] = username
            return token
        else:
            self.failed_attempts[ip_address] = self.failed_attempts.get(ip_address, 0) + 1
            return None

    def logout(self, token):
        if token in self.active_sessions:
            del self.active_sessions[token]
            return True
        return False

    def validate_session(self, token):
        return token in self.active_sessions


class ReportGenerator:
    def __init__(self, company_name, fiscal_year):
        self.company_name = company_name
        self.fiscal_year = fiscal_year
        self.data = []

    def load_data(self, records):
        self.data = records

    def calculate_summary(self, data, include_tax):
        total = sum(r['amount'] for r in data)
        avg = total / len(data) if data else 0
        if include_tax:
            total = total * 1.18
        return round(total, 2), round(avg, 2)

    def generate_monthly_report(self, month, include_tax):
        filtered = [r for r in self.data if r['month'] == month]
        total, avg = self.calculate_summary(filtered, include_tax)
        return {
            'company': self.company_name,
            'month': month,
            'total': total,
            'average': avg,
            'records': len(filtered)
        }


def calculate_discount(price, discount_percent, is_member):
    if is_member:
        discount_percent += 5
    discount = (discount_percent / 100) * price
    return round(price - discount, 2)


def parse_date_string(date_str, fmt):
    try:
        return datetime.datetime.strptime(date_str, fmt)
    except ValueError:
        return None


def compute_shipping_cost(weight, distance, is_express):
    base = 2.5
    rate = 0.05 if not is_express else 0.12
    cost = base + (weight * rate) + (distance * 0.01)
    return round(cost, 2)


def find_nearest_warehouse(customer_lat, customer_lon, warehouses):
    nearest = None
    min_dist = float('inf')
    for wh in warehouses:
        dist = math.sqrt((customer_lat - wh['lat'])**2 + (customer_lon - wh['lon'])**2)
        if dist < min_dist:
            min_dist = dist
            nearest = wh
    return nearest
