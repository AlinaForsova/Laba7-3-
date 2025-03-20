import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import org.bson.conversions.Bson;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Updates.combine;
import static com.mongodb.client.model.Updates.set;

public class Main {
    private MongoCollection<Document> collection;
    private JFrame frame;
    private JTable table;
    private DefaultTableModel tableModel;
    private JTextField idField, lastNameField, districtField, discountField;

    public static void main(String[] args) {
        new Main().createAndShowGUI();
    }

    private void createAndShowGUI() {
        try {

            String uri = "mongodb://localhost:27017";
            MongoClient mongoClient = MongoClients.create(uri);
            MongoDatabase database = mongoClient.getDatabase("Shop");
            collection = database.getCollection("Customers");

            frame = new JFrame("Управление покупателями");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(800, 600);

            String[] columnNames = {"Идентификатор", "Фамилия", "Район проживания", "Скидка (%)"};
            tableModel = new DefaultTableModel(columnNames, 0);
            table = new JTable(tableModel);

            JPanel inputPanel = new JPanel();
            inputPanel.setLayout(new GridLayout(6, 2));

            idField = new JTextField();
            lastNameField = new JTextField();
            districtField = new JTextField();
            discountField = new JTextField();

            inputPanel.add(new JLabel("Идентификатор:"));
            inputPanel.add(idField);
            inputPanel.add(new JLabel("Фамилия:"));
            inputPanel.add(lastNameField);
            inputPanel.add(new JLabel("Район проживания:"));
            inputPanel.add(districtField);
            inputPanel.add(new JLabel("Скидка (%):"));
            inputPanel.add(discountField);

            JButton addButton = new JButton("Добавить");
            JButton updateButton = new JButton("Изменить");
            JButton deleteButton = new JButton("Удалить");
            JButton clearButton = new JButton("Очистить таблицу");

            inputPanel.add(addButton);
            inputPanel.add(updateButton);
            inputPanel.add(deleteButton);
            inputPanel.add(clearButton);

            JScrollPane scrollPane = new JScrollPane(table);

            frame.add(inputPanel, BorderLayout.NORTH);
            frame.add(scrollPane, BorderLayout.CENTER);

            addButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    addCustomer(
                            idField.getText(),
                            lastNameField.getText(),
                            districtField.getText(),
                            discountField.getText()
                    );
                }
            });

            updateButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    showUpdateDialog();
                }
            });

            deleteButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    showDeleteDialog();
                }
            });

            clearButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    clearTable();
                }
            });

            loadData();

            frame.setVisible(true);
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(frame, "Ошибка подключения к MongoDB: " + e.getMessage(), "Ошибка", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadData() {
        tableModel.setRowCount(0); // Очистка таблицы
        for (Document doc : collection.find()) {
            tableModel.addRow(new Object[]{
                    doc.getString("id"),
                    doc.getString("lastName"),
                    doc.getString("district"),
                    doc.getInteger("discount")
            });
        }
    }

    private void addCustomer(String id, String lastName, String district, String discount) {
        if (id.isEmpty() || lastName.isEmpty() || district.isEmpty() || discount.isEmpty()) {
            showAlert("Ошибка", "Все поля должны быть заполнены!");
            return;
        }

        try {
            Document doc = new Document("id", id)
                    .append("lastName", lastName)
                    .append("district", district)
                    .append("discount", Integer.parseInt(discount));
            collection.insertOne(doc);
            loadData();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Ошибка", "Не удалось добавить покупателя: " + e.getMessage());
        }
    }

    private void showUpdateDialog() {
        JTextField searchLastNameField = new JTextField();
        Object[] message = {
                "Введите фамилию для поиска:", searchLastNameField
        };

        int option = JOptionPane.showConfirmDialog(frame, message, "Поиск покупателя", JOptionPane.OK_CANCEL_OPTION);
        if (option == JOptionPane.OK_OPTION) {
            String searchLastName = searchLastNameField.getText();
            if (searchLastName.isEmpty()) {
                showAlert("Ошибка", "Фамилия для поиска должна быть заполнена!");
                return;
            }

            Document customer = collection.find(eq("lastName", searchLastName)).first();
            if (customer == null) {
                showAlert("Ошибка", "Покупатель не найден!");
                return;
            }

            JTextField newIdField = new JTextField(customer.getString("id"));
            JTextField newLastNameField = new JTextField(customer.getString("lastName"));
            JTextField newDistrictField = new JTextField(customer.getString("district"));
            JTextField newDiscountField = new JTextField(customer.getInteger("discount").toString());

            Object[] updateMessage = {
                    "Идентификатор:", newIdField,
                    "Фамилия:", newLastNameField,
                    "Район проживания:", newDistrictField,
                    "Скидка (%):", newDiscountField
            };

            int updateOption = JOptionPane.showConfirmDialog(frame, updateMessage, "Обновление данных", JOptionPane.OK_CANCEL_OPTION);
            if (updateOption == JOptionPane.OK_OPTION) {
                updateCustomer(
                        searchLastName,
                        newIdField.getText(),
                        newLastNameField.getText(),
                        newDistrictField.getText(),
                        newDiscountField.getText()
                );
            }
        }
    }

    private void updateCustomer(String searchLastName, String newId, String newLastName, String newDistrict, String newDiscount) {
        if (newId.isEmpty() || newLastName.isEmpty() || newDistrict.isEmpty() || newDiscount.isEmpty()) {
            showAlert("Ошибка", "Все поля должны быть заполнены!");
            return;
        }

        try {
            Bson filter = eq("lastName", searchLastName);
            Bson update = combine(
                    set("id", newId),
                    set("lastName", newLastName),
                    set("district", newDistrict),
                    set("discount", Integer.parseInt(newDiscount))
            );
            UpdateResult result = collection.updateOne(filter, update);

            if (result.getModifiedCount() > 0) {
                loadData();
            } else {
                showAlert("Ошибка", "Покупатель не найден!");
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Ошибка", "Не удалось обновить покупателя: " + e.getMessage());
        }
    }

    private void showDeleteDialog() {
        JTextField deleteLastNameField = new JTextField();
        Object[] message = {
                "Введите фамилию для удаления:", deleteLastNameField
        };

        int option = JOptionPane.showConfirmDialog(frame, message, "Удаление покупателя", JOptionPane.OK_CANCEL_OPTION);
        if (option == JOptionPane.OK_OPTION) {
            String deleteLastName = deleteLastNameField.getText();
            if (deleteLastName.isEmpty()) {
                showAlert("Ошибка", "Фамилия для удаления должна быть заполнена!");
                return;
            }

            deleteCustomer(deleteLastName);
        }
    }

    private void deleteCustomer(String lastName) {
        try {
            Bson filter = eq("lastName", lastName);
            DeleteResult result = collection.deleteOne(filter);

            if (result.getDeletedCount() > 0) {
                loadData();
                showAlert("Успех", "Покупатель успешно удален!");
            } else {
                showAlert("Ошибка", "Покупатель не найден!");
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Ошибка", "Не удалось удалить покупателя: " + e.getMessage());
        }
    }

    private void clearTable() {
        int option = JOptionPane.showConfirmDialog(frame, "Вы уверены, что хотите очистить таблицу?", "Очистка таблицы", JOptionPane.YES_NO_OPTION);
        if (option == JOptionPane.YES_OPTION) {
            try {
                collection.deleteMany(new Document());
                // Очистка таблицы в интерфейсе
                tableModel.setRowCount(0);
                showAlert("Успех", "Таблица успешно очищена!");
            } catch (Exception e) {
                e.printStackTrace();
                showAlert("Ошибка", "Не удалось очистить таблицу: " + e.getMessage());
            }
        }
    }

    private void showAlert(String title, String message) {
        JOptionPane.showMessageDialog(frame, message, title, JOptionPane.INFORMATION_MESSAGE);
    }
}
