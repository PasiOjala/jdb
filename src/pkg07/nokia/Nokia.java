package pkg07.nokia;

import com.sun.xml.internal.fastinfoset.util.StringArray;
import java.io.File;
import java.io.FileNotFoundException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Scanner;

public class Nokia {
    private static final String fileName = 
            "NOK1V-2000-01-01-2014-31-12.csv";
    
    private final Scanner fs;
    private String kenttäerotin = ",";
    private String seuraavaRivi;
    private String[] otsikot;
    private String vientilause;
    
    private Connection db;
    
    
    private Nokia() throws FileNotFoundException, SQLException {
        fs = new Scanner(new File(fileName));
        haeSeuraavaRivi();
        db=DriverManager.getConnection(
                "jdbc:derby://localhost:1527/07-nokia",
                "nokia", "nokia");
    }
    
    private void haeSeuraavaRivi() {
        if (fs.hasNextLine()) {
            seuraavaRivi = fs.nextLine();
        } else {
            seuraavaRivi = null;
        }
    }
    
    private void lueSep() {
        if (seuraavaRivi==null) return;
        if (seuraavaRivi.startsWith("sep=")) {
            kenttäerotin = seuraavaRivi.substring(4);
            haeSeuraavaRivi();
            System.out.println("Fieldsep schanged to "+kenttäerotin);
        }
    }
    
    private void lueOtsikot() throws SQLException{
        if (seuraavaRivi==null) return;
        otsikot=seuraavaRivi.split(kenttäerotin);
//        System.out.print("Otsikot: ");
//        for (String otsikko:otsikot){
//            System.out.print(otsikko+" ");
//        }
        for(int i=0;i<otsikot.length;i++){
            otsikot[i]=otsikot[i].toLowerCase().replace(" ", "_");
            if (otsikot[i].equals("date")){
                otsikot[i]="tradedate";
            }
        }
        System.out.println("Otsikot: "+String.join(" ",otsikot));
        System.out.println("");
        haeSeuraavaRivi();
        
        //poista vanha
        ResultSet taulut=
                db.getMetaData().getTables(null, null, "KURSSIT", null);
        if (taulut.next()){
            System.out.println(taulut.getString("TABLE_NAME"));
            db.createStatement().execute("drop table kurssit");
        }else{
            System.out.println("ei taulua nimeltä \"kurssit\"");
        }
        //luo uusi taulu ja vientilause
        
        StringBuilder luontilause=new StringBuilder();
        StringBuilder vientilause=new StringBuilder();
        luontilause.append("create table kurssit(");
        vientilause.append("insert into kurssit values (");
        for (String sarake:otsikot){
            if (sarake.equals("tradedate")){
            vientilause.append("?");
            luontilause.append(sarake).append(" "+" date primary key");
            }
            else if (!sarake.isEmpty()){
                luontilause.append(" , ")
                .append(sarake)
                .append(" "+" decimal (16,4)");
                vientilause.append(",?");
            }
            
        }
        vientilause.append(")");
        luontilause.append(")");
        this.vientilause=vientilause.toString();
//        System.out.println(luontilause);
//        System.out.println(vientilause);
        db.createStatement().execute(luontilause.toString());
        
        
    }
    private void tallennaKurssit() throws SQLException {
        
        PreparedStatement ps = db.prepareStatement(vientilause);
        while (seuraavaRivi!=null) {
                //tallennetaan rivi
            
            String kentät[]=seuraavaRivi.split(kenttäerotin);
            ps.setDate(1,Date.valueOf(kentät[0]) );
            try{
            for (int i=1;i<kentät.length;i++){
                kentät[i]=kentät[i].replace(",", ".");
                if (kentät[i].isEmpty()){
                    ps.setBigDecimal(i+1,null);
                }
                else{
                    ps.setBigDecimal(i+1,new BigDecimal(kentät[i]));
                }
            }
            }catch(Exception ex){
                ex.printStackTrace();
            }
            ps.execute();
            
            haeSeuraavaRivi();
            
        }
//        db.commit();
    }
    public static void main(String[] args) throws Exception {
        Nokia nokia = new Nokia();
        nokia.lueSep();
        nokia.lueOtsikot();
        nokia.tallennaKurssit();
    }
    
}
