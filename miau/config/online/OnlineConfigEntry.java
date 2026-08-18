package miau.config.online;

public class OnlineConfigEntry {
    public String setting_id;
    public String config_id;
    public String name;
    public String setting_type;
    public String description;
    public String date;
    public String contributors;
    public String status_type;
    public String status_date;
    public String version;
    public int load_count;

    public String getId() {
        if (this.config_id != null && !this.config_id.trim().isEmpty()) {
            return this.config_id;
        } else {
            return this.setting_id == null ? "" : this.setting_id;
        }
    }

    public String getName() {
        return this.name != null && !this.name.trim().isEmpty() ? this.name : this.getId();
    }

    public String getAuthor() {
        return this.contributors != null && !this.contributors.trim().isEmpty() ? this.contributors : "unknown";
    }

    public int getLoadCount() {
        return Math.max(0, this.load_count);
    }

    public String getVersion() {
        return this.version != null && !this.version.trim().isEmpty() ? this.version : "";
    }
}
