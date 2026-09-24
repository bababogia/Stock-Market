public class UserAccount {
    private double balance;

    public UserAccount() {
        this.balance = 0;
    }

    public void loadMoney(double amount) {
        if (amount > 0) {
            balance += amount;
        }
    }

    public boolean canAfford(double amount) {
        return balance >= amount;
    }

    public void deduct(double amount) {
        if (canAfford(amount)) {
            balance -= amount;
        }
    }

    public double getBalance() {
        return balance;
    }
}
