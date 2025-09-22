import java.util.*;

/**
 * Категории товара (enum).
 */
enum Category { STANDARD, PREMIUM, ENTERPRISE }

/**
 * Доменная модель товара.
 * Содержит 5 полей разных типов:
 * int, String, float, enum, boolean.
 */
class Product {
    private int id;            // int — идентификатор
    private String name;       // String — название
    private float price;       // float — цена (в евро)
    private Category category; // enum — категория
    private boolean inStock;   // boolean — наличие на складе

    public Product(int id, String name, float price, Category category, boolean inStock) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.category = category;
        this.inStock = inStock;
    }

    // Инкапсуляция: геттеры/сеттеры
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public float getPrice() { return price; }
    public void setPrice(float price) { this.price = price; }
    public Category getCategory() { return category; }
    public void setCategory(Category c) { this.category = c; }
    public boolean isInStock() { return inStock; }
    public void setInStock(boolean b) { this.inStock = b; }
}

/**
 * Абстракция "базы данных" для работы с товарами.
 */
interface ProductRepository {
    /** Добавить товар */
    void add(Product p);

    /** Вернуть все товары */
    List<Product> findAll();

    /** Поиск по имени (пример поиска по одному полю) */
    List<Product> findByName(String name);
}

/**
 * Простая реализация "базы данных" в памяти (список).
 */
class InMemoryProductRepository implements ProductRepository {
    private final List<Product> data = new ArrayList<>();

    @Override
    public void add(Product p) { data.add(p); }

    @Override
    public List<Product> findAll() { return Collections.unmodifiableList(data); }

    @Override
    public List<Product> findByName(String name) {
        List<Product> out = new ArrayList<>();
        for (Product p : data) {
            if (p.getName().equals(name)) out.add(p);
        }
        return out;
    }
}

/** Простая булевая маска полей (как в задании: сначала как bool). */
class BoolFieldMask {
    public boolean id;
    public boolean name;
    public boolean price;
    public boolean category;
    public boolean inStock;

    public static BoolFieldMask all() {
        var m = new BoolFieldMask();
        m.id = m.name = m.price = m.category = m.inStock = true;
        return m;
    }
    public static BoolFieldMask none() { return new BoolFieldMask(); }
}

/**
 * Маска для выбора полей объекта Product.
 * Реализована через биты одного int.
 */
class FieldMask {
    public static final int ID       = 1 << 0;
    public static final int NAME     = 1 << 1;
    public static final int PRICE    = 1 << 2;
    public static final int CATEGORY = 1 << 3;
    public static final int INSTOCK  = 1 << 4;

    public static int all()  { return ID | NAME | PRICE | CATEGORY | INSTOCK; }
    public static int none() { return 0; }

    public static int union(int a, int b)     { return a | b; }
    public static int intersect(int a, int b) { return a & b; }
    public static int minus(int a, int b)     { return a & ~b; }

    /**
     * Вывести в консоль объект Product согласно БИТОВОЙ маске.
     */
    public static void print(Product p, int mask) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        if ((mask & ID) != 0)       first = append(sb, first, "id", p.getId());
        if ((mask & NAME) != 0)     first = append(sb, first, "name", '"' + p.getName() + '"');
        if ((mask & PRICE) != 0)    first = append(sb, first, "price(€)", p.getPrice());
        if ((mask & CATEGORY) != 0) first = append(sb, first, "category", p.getCategory());
        if ((mask & INSTOCK) != 0)  first = append(sb, first, "inStock", p.isInStock());
        sb.append("}\n");
        System.out.print(sb.toString());
    }

    /**
     * Вывести в консоль объект Product согласно БУЛЕВОЙ маске (п.3: сначала как bool).
     */
    public static void print(Product p, BoolFieldMask m) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        if (m.id)       first = append(sb, first, "id", p.getId());
        if (m.name)     first = append(sb, first, "name", '"' + p.getName() + '"');
        if (m.price)    first = append(sb, first, "price(€)", p.getPrice());
        if (m.category) first = append(sb, first, "category", p.getCategory());
        if (m.inStock)  first = append(sb, first, "inStock", p.isInStock());
        sb.append("}\n");
        System.out.print(sb.toString());
    }

    private static boolean append(StringBuilder sb, boolean first, String k, Object v) {
        if (!first) sb.append(", ");
        sb.append(k).append(": ").append(v);
        return false;
    }

    /**
     * Копировать выбранные поля из source в target (битовая маска).
     */
    public static void copy(Product source, Product target, int mask) {
        if ((mask & ID) != 0)       target.setId(source.getId());
        if ((mask & NAME) != 0)     target.setName(source.getName());
        if ((mask & PRICE) != 0)    target.setPrice(source.getPrice());
        if ((mask & CATEGORY) != 0) target.setCategory(source.getCategory());
        if ((mask & INSTOCK) != 0)  target.setInStock(source.isInStock());
    }
}

/**
 * Демонстрация:
 * - поиск по имени
 * - печать по маске (bool и битовая)
 * - операции с масками
 * - копирование по маске.
 */
public class Main {
    public static void main(String[] args) {
        ProductRepository repo = new InMemoryProductRepository();

        // Товары с ценами в евро
        repo.add(new Product(1, "iPhone 17",   1299.00f, Category.PREMIUM,    true));
        repo.add(new Product(2, "MacBook Air", 1499.00f, Category.ENTERPRISE, true));
        repo.add(new Product(3, "iPad 10",      579.00f, Category.STANDARD,   true));
        repo.add(new Product(4, "iPhone 17",   1349.00f, Category.PREMIUM,    false));

        // Поиск по одному полю
        var iphones = repo.findByName("iPhone 17");
        System.out.println("findByName('iPhone 17') -> " + iphones.size() + " шт.");

        // Печать по БИТОВОЙ маске (NAME + PRICE)
        int maskNamePrice = FieldMask.union(FieldMask.NAME, FieldMask.PRICE);
        for (Product p : repo.findAll()) FieldMask.print(p, maskNamePrice);

        // Печать по БУЛЕВОЙ маске (NAME + PRICE)
        var boolMask = BoolFieldMask.none();
        boolMask.name = true;
        boolMask.price = true;
        System.out.println("bool mask (name+price):");
        FieldMask.print(repo.findAll().get(0), boolMask);

        // Маска: всё кроме ID (битовая)
        int maskAllButId = FieldMask.minus(FieldMask.all(), FieldMask.ID);
        System.out.println("all but id:");
        FieldMask.print(repo.findAll().get(0), maskAllButId);

        // Копирование цены у одинаковых по имени (iPhone 17) — по БИТОВОЙ маске
        if (iphones.size() >= 2) {
            Product src = iphones.get(1), dst = iphones.get(0);
            FieldMask.copy(src, dst, FieldMask.PRICE);
            System.out.println("after price copy:");
            FieldMask.print(dst, FieldMask.union(FieldMask.NAME, FieldMask.PRICE));
        }

        System.out.println("Done.");
    }
}
