import java.sql.*;
public class DbQuery {
  public static void main(String[] args) throws Exception {
    Class.forName("org.sqlite.JDBC");
    try (Connection c = DriverManager.getConnection("jdbc:sqlite:app.db")) {
      String q = args.length>0?args[0]:"SELECT name FROM sqlite_master WHERE type='table'";
      try (Statement s = c.createStatement(); ResultSet rs = s.executeQuery(q)) {
        while (rs.next()) System.out.println(rs.getString(1));
      }
    }
  }
}
