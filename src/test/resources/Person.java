package tests;

import org.joeyb.freemapper.Field;
import org.joeyb.freemapper.FreeMapper;

import java.util.Date;
import java.util.Optional;

@FreeMapper
public interface Person {

    @Field(name = "person_name")
    String getName();

    boolean isAlive();

    Optional<Integer> getAge();

    Date getCreatedAt();

    // This builder is not very useful and intended just for testing.
    class Builder {
        private String name;
        private boolean alive;
        private Optional<Integer> age;
        private Date createdAt;

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder setAlive(Boolean alive) {
            this.alive = alive;
            return this;
        }

        public Builder setAge(Optional<Integer> age) {
            this.age = age;
            return this;
        }

        public Builder setCreatedAt(Date createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Person build() {
            return new Person() {
                @Override
                public String getName() {
                    return name;
                }

                @Override
                public boolean isAlive() {
                    return alive;
                }

                @Override
                public Optional<Integer> getAge() {
                    return age;
                }

                @Override
                public Date getCreatedAt() {
                    return createdAt;
                }
            };
        }
    }
}